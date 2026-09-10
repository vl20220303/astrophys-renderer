package utils.renderer.astrophys.camera;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import utils.renderer.astrophys.utils.BigColor;
import utils.renderer.astrophys.utils.ColorLayer;
import utils.renderer.astrophys.utils.Particle;
import utils.renderer.astrophys.utils.Vector;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

public class RayTracer extends Camera{
    private int pixelHeight = 1, pixelWidth = 1;
    public double focalLength;

    private final int THREAD_COUNT = Math.max(Runtime.getRuntime().availableProcessors()-1,1);
    private ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

    public RayTracer(Vector pos, Vector normal){
        super(pos, normal);
        focalLength = normal.abs()*2;
    }

    public RayTracer(Vector pos){
        this(pos, pos.scale(-1));
    }

    @Override
    public void zoom(double ox, double oy, double ticks) {
        Vector origin = pos.add(normal.scale(zoom));
        zoom = Math.min(Math.max(zoom*Math.pow(1.05, ticks), 1e-8), 1e8);
        pos = origin.subtract(normal.scale(zoom));
    }

    @Override
    public void scale(double ticks) {
        scale = Math.min(Math.max(scale+ticks, 1e-8), 1e8);
    }

    @Override
    public void focus(double ticks){
        focalLength = Math.min(Math.max(focalLength*(1-ticks), 1e-8), 1e8);
    }

    @Override
    public void shift(double dx, double dy) {
        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        up = right.cross(normal).normalize();

        Vector shiftVec = right.scale(dx).addInPlace(up.scale(dy));

        pos = pos.add(shiftVec);
    }
    
    @Override
    public void jump(double dx, double dy) {
        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        up = right.cross(normal).normalize();

        Vector shiftVec = right.scale(dx).addInPlace(up.scale(dy));

        pos.addInPlace(shiftVec);
    }

    @Override
    public void orbit(double dx, double dy) {
        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        
        if(1-Math.pow(normal.dot(up),2) < Math.pow(dy, 2)) {
            dy*=Math.max(0, -Math.signum(normal.dot(up)*dy));
        }

        Vector origin = pos.add(normal.scale(zoom));
        normal = normal.rotateAroundAxis(up, dx).rotateAroundAxis(right, dy).normalize();
        pos = origin.subtract(normal.scale(zoom));
    }

    @Override
    public void render(ArrayList<Particle> particles, Graphics2D g){
        final Vector right = normal.cross(Vector.Y_AXIS).normalize();
        final Vector up = right.cross(normal).normalize();
        Vector focus = normal.scale(-focalLength);

        final long t0 = System.nanoTime();

        final int HEIGHT = (int) (environment.RESOLUTION), WIDTH = (int) (HEIGHT * environment.ASPECT_RATIO);
        final Color[][] buf = new Color[WIDTH][HEIGHT];

        final int[] indices = new int[particles.size()];
        for(int i = 0; i<indices.length; i++){
            indices[i] = i;
        }
        final BvhNode node = buildBvhNode(particles, indices, 0, particles.size());

        final long t1 = System.nanoTime();

        int TASKS_PER_THREAD = (particles.size() < 50) ? 16 : ((particles.size() < 100) ? 4 : 2);
        int columns = WIDTH/pixelWidth;
        int taskCount = THREAD_COUNT * TASKS_PER_THREAD;
        List<Future<?>> futures = new ArrayList<>(taskCount);

        final Vector stepUp = up.scale(pixelHeight / scale), stepRight = right.scale(pixelWidth / scale);

        for(int n = 0; n<taskCount; n++){
            final int firstColumn = n * columns / taskCount;
            final int lastColumn = (n + 1) * columns / taskCount;
            final int startCol = firstColumn * pixelWidth - WIDTH/2;
            final int endCol = lastColumn * pixelWidth - WIDTH/2;
            futures.add(executor.submit(() -> {
                Vector base = pos.add(right.scale(startCol+pixelWidth/2).addInPlace(up.scale(-HEIGHT/2+pixelHeight/2))).scaleInPlace(1/scale);
                Vector pixel = new Vector(Vector.ORIGIN);
                for(int i = startCol; i<endCol; i+=pixelWidth){
                    pixel.copy(base);
                    for(int j = -HEIGHT/2; j<HEIGHT/2; j+=pixelHeight){
                        buf[i+WIDTH/2][j+HEIGHT/2] = getColor(focus, pixel, particles, node, indices);
                        pixel.addInPlace(stepUp);
                    }
                    base.addInPlace(stepRight);
                }
            }));
        }

        for(Future<?> future : futures){
            try{
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ExecutionException e){
                e.printStackTrace();
            }
        }

        final long t2 = System.nanoTime();

        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        int[] pixels = ((java.awt.image.DataBufferInt) img.getRaster().getDataBuffer()).getData();

        for(int x = 0; x < WIDTH; x += pixelWidth) {
            for(int y = 0; y < HEIGHT; y += pixelHeight) {
                int rgb = buf[x][y].getRGB();

                for (int dx = 0; dx < pixelWidth; dx++) {
                    int px = x + dx;
                    if (px >= WIDTH) break;

                    for (int dy = 0; dy < pixelHeight; dy++) {
                        int py = y + dy;
                        if (py >= HEIGHT) break;
                        pixels[py * WIDTH + px] = rgb;
                    }
                }
            }
        }

        g.drawImage(img, -WIDTH / 2, -HEIGHT / 2, null);
        final long t3 = System.nanoTime();
        System.out.printf("%s BREAKDOWN %s| build: %.3f, trace: %.3f, paint: %.3f \n", "\u001B[35m", "\u001B[0m", (t1-t0)/1e6, (t2-t1)/1e6, (t3-t2)/1e6);
    }

    class BvhNode {
        double minX = Double.POSITIVE_INFINITY, 
               minY = Double.POSITIVE_INFINITY, 
               minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, 
               maxY = Double.NEGATIVE_INFINITY, 
               maxZ = Double.NEGATIVE_INFINITY;
        BvhNode left, right;
        int start, count;
        boolean isLeaf;
    }

    private static final int LEAF_SIZE = 4;

    private BvhNode buildBvhNode(ArrayList<Particle> particles, int[] indices, int start, int count){
        BvhNode node = new BvhNode();
        for(int i = start; i<start+count; i++){
            Particle p = particles.get(indices[i]);
            node.minX = Math.min(node.minX, p.pos.x - p.rad);
            node.minY = Math.min(node.minY, p.pos.y - p.rad);
            node.minZ = Math.min(node.minZ, p.pos.z - p.rad);

            node.maxX = Math.max(node.maxX, p.pos.x + p.rad);
            node.maxY = Math.max(node.maxY, p.pos.y + p.rad);
            node.maxZ = Math.max(node.maxZ, p.pos.z + p.rad);
        }
        
        if(count <= LEAF_SIZE){
            node.isLeaf = true;
            node.start = start;
            node.count = count;
            return node;
        }

        double extentX = node.maxX - node.minX,
               extentY = node.maxY - node.minY,
               extentZ = node.maxZ - node.minZ;
        double maxExtent = Math.max(extentX, Math.max(extentY, extentZ));
        int axis = 0;
        if(maxExtent==extentY) axis = 1;
        else if(maxExtent==extentZ) axis = 2;

        quickSelect(particles, indices, start, start+count-1, axis);
        
        int leftSize = count/2, rightSize = count - leftSize;

        node.left = buildBvhNode(particles, indices, start, leftSize);
        node.right = buildBvhNode(particles, indices, start+leftSize, rightSize);
        return node;
    }

    private int partition(ArrayList<Particle> particles, int[] indices, int low, int high, int axis){
        int mid = low + high >>> 1;
        double pivot = axis == 0 ? particles.get(indices[mid]).pos.x : 
                       axis == 1 ? particles.get(indices[mid]).pos.y : particles.get(indices[mid]).pos.z;
        
        while(low<=high){
            while(true){
                double p = axis == 0 ? particles.get(indices[low]).pos.x : 
                           axis == 1 ? particles.get(indices[low]).pos.y : particles.get(indices[low]).pos.z;
                if(p >= pivot) break;
                low++;
            }

            while(true){
                double p = axis == 0 ? particles.get(indices[high]).pos.x : 
                           axis == 1 ? particles.get(indices[high]).pos.y : particles.get(indices[high]).pos.z;
                if(p <= pivot) break;
                high--;
            }

            if (low<=high){
                int tmp = indices[low];
                indices[low] = indices[high];
                indices[high] = tmp;
                low++;
                high--;
            }
        }

        return low;
    }

    private void quickSelect(ArrayList<Particle> particles, int[] indices, int low, int high, int axis){
        int mid = (low + high) >>> 1;
        while(low < high){
            int pivotIdx = partition(particles, indices, low, high, axis);
            if(mid < pivotIdx){
                high = pivotIdx - 1;
            } else{
                low = pivotIdx;
            }
        }
    }

    private Color getColor(Vector focus, Vector pixel, ArrayList<Particle> particles, BvhNode node, int[] indices){
        Vector origin = pixel;
        Vector ray = pixel.subtract(focus).normalizeInPlace();

        int reflections = 0, maxReflections = !useLighting ? 1 : 5;
        ColorLayer[] layers = new ColorLayer[2*maxReflections+1];
        int head = 0;

        Intersection intersection; Particle particle;
        Vector newOrigin, incoming1, normal;

        while(reflections<maxReflections){
            intersection = new Intersection(null, null, Double.POSITIVE_INFINITY);
            getIntersection(origin, ray, particles, indices, node, intersection);
            particle = intersection.intersectParticle;
            newOrigin = intersection.intersectPoint;
            double dist = intersection.intersectDist;

            if(particle==null){ break; }

            if(!useLighting){
                return particle.color;
            }

            if(reflections == 0) dist = 0;
            if(particle.intensity == 0){
                if(head>0 && layers[head-1].operation == ColorLayer.opType.MULTIPLY){
                    layers[head-1].compress(particle.color, dist);
                } else{
                    layers[head++] = new ColorLayer(particle.color, dist);
                }
            } else{
                layers[head++] = new ColorLayer(particle.color, dist, particle.intensity);
                layers[head++] = new ColorLayer(particle.color, dist);
            }

            incoming1 = ray.scale(-1).normalize();
            normal = particle.pos.subtract(newOrigin).normalize();
            ray = incoming1.add(normal.scale(2*incoming1.dot(normal))).normalize();
            origin = newOrigin;

            reflections++;
        }

        BigColor color = new BigColor(environment.BACKGROUND_COLOR);
        color.scale(1e100);

        ColorLayer layer;
        while(head>0){
            layer = layers[--head];
            if(layer.operation == ColorLayer.opType.ADD){
                color.add(layer.color, layer.intensity);
                color.scale(layer.dropoff);
            } else{
                color.multiply(layer.color, layer.dropoff);
            }
        }
        return color.normalize();
    }
    
    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;

    private class Intersection{
        Particle intersectParticle;
        Vector   intersectPoint;
        double   intersectDist;
        Intersection(Particle p, Vector v, double d){
            this.intersectParticle = p;
            this.intersectPoint = v;
            this.intersectDist = d;
        }
    }

    private double intersectsBVH(Vector rayOrigin, double invX, double invY, double invZ, BvhNode node, double maxDist){
        double t1 = (node.maxX - rayOrigin.x)*invX,
               t2 = (node.minX - rayOrigin.x)*invX,
               t3 = (node.maxY - rayOrigin.y)*invY,
               t4 = (node.minY - rayOrigin.y)*invY,
               t5 = (node.maxZ - rayOrigin.z)*invZ,
               t6 = (node.minZ - rayOrigin.z)*invZ;
        double tmin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6));
        double tmax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));
        if(tmax > 0 && tmin <= tmax && tmin <= maxDist) return tmin;
        return Double.POSITIVE_INFINITY;
    }

    private void getIntersection(Vector rayOrigin, Vector rayVec, ArrayList<Particle> particles, int[] indices, BvhNode node, Intersection intersection){
        double invX = 1/rayVec.x, invY = 1/rayVec.y, invZ = 1/rayVec.z;
        double a = rayVec.dot(rayVec);
        Particle p;

        BvhNode[] stack = new BvhNode[64];
        int head = 0;
        stack[head++] = node;
        
        var A = DoubleVector.broadcast(SPECIES, a);

        var Ox = DoubleVector.broadcast(SPECIES, rayOrigin.x);
        var Oy = DoubleVector.broadcast(SPECIES, rayOrigin.y);
        var Oz = DoubleVector.broadcast(SPECIES, rayOrigin.z);

        var Rx = DoubleVector.broadcast(SPECIES, rayVec.x);
        var Ry = DoubleVector.broadcast(SPECIES, rayVec.y);
        var Rz = DoubleVector.broadcast(SPECIES, rayVec.z);

        while(head>0){
            BvhNode current = stack[--head];
            if(current.isLeaf){
                double[] x = new double[current.count];
                double[] y = new double[current.count];
                double[] z = new double[current.count];
                double[] r = new double[current.count];
                
                for(int i = 0; i<current.count; i++){
                    p = particles.get(indices[current.start+i]);
                    x[i] = p.pos.x; y[i] = p.pos.y; z[i] = p.pos.z; r[i] = p.rad;
                }

                double[] discriminants = new double[current.count];
                double[] dists = new double[current.count];
                double[] alts = new double[current.count];

                for(int offset = 0; offset < current.count; offset += SPECIES.length()){
                    var mask = SPECIES.indexInRange(offset, current.count);
                    var X = DoubleVector.fromArray(SPECIES, x, offset, mask);
                    var Y = DoubleVector.fromArray(SPECIES, y, offset, mask);
                    var Z = DoubleVector.fromArray(SPECIES, z, offset, mask);
                    var R = DoubleVector.fromArray(SPECIES, r, offset, mask);

                    var Dx = Ox.sub(X); var Dy = Oy.sub(Y); var Dz = Oz.sub(Z);
                    var B = Dx.fma(Rx, Dy.fma(Ry, Dz.mul(Rz))).mul(2);
                    var C = Dx.fma(Dx, Dy.fma(Dy, Dz.mul(Dz))).sub(R.mul(R));
                    var Dscrm = B.mul(B).sub(A.mul(C).mul(4));
                    var sqrtD = Dscrm.lanewise(VectorOperators.SQRT, mask);
                    var Dist = B.add(sqrtD).div(A).div(-2);
                    var Alt = Dist.add(B.div(A)).mul(-1);

                    Dscrm.intoArray(discriminants, offset, mask);
                    Dist.intoArray(dists, offset, mask);
                    Alt.intoArray(alts, offset, mask);
                }

                for(int i = 0; i<current.count; i++){
                    double discriminant = discriminants[i];
                    double dist = dists[i];
                    double alt = alts[i];
                    if(discriminant < 0) continue;
                    if(dist < 1e-9) dist = alt;

                    if(dist < 1e-9) continue;
                    if(dist > intersection.intersectDist) continue;
                    intersection.intersectParticle = particles.get(indices[current.start + i]);
                    intersection.intersectDist = dist;
                }
                continue;
            }

            double tRight = intersectsBVH(rayOrigin, invX, invY, invZ, current.right, intersection.intersectDist);
            double tLeft = intersectsBVH(rayOrigin, invX, invY, invZ, current.left, intersection.intersectDist);
            boolean hitRight = tRight != Double.POSITIVE_INFINITY;
            boolean hitLeft  = tLeft  != Double.POSITIVE_INFINITY;

            if (hitRight && hitLeft) {
                if (tRight < tLeft) {
                    stack[head++] = current.left;
                    stack[head++] = current.right;
                } else {
                    stack[head++] = current.right;
                    stack[head++] = current.left;
                }
            }
            else if (hitRight) stack[head++] = current.right;
            else if (hitLeft) stack[head++] = current.left;
        }
        if(intersection.intersectParticle != null) 
            intersection.intersectPoint = rayOrigin.add(rayVec.scale(intersection.intersectDist));
    }

    @Override
    protected Point[] projectLineToScreen(Vector p, Vector q) {
        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        up = right.cross(normal).normalize();

        Vector pRel = p.subtract(pos);
        Vector qRel = q.subtract(pos);
        double pDepth = pRel.dot(normal);
        double qDepth = qRel.dot(normal);

        //line behind plane
        if (pDepth < 0 && qDepth < 0) {
            return null;
        }

        //clip line
        if ((pDepth < 0 || qDepth < 0)) {
            double t = pDepth / (pDepth - qDepth);
            if (pDepth < 0) {
                pRel = pRel.add(qRel.subtract(pRel).scale(t));
                pDepth = 0;
            } else {
                qRel = qRel.add(pRel.subtract(qRel).scale(1 - t));
                qDepth = 0;
            }
        }

        double px = pRel.dot(right); double py = pRel.dot(up);
        double qx = qRel.dot(right); double qy = qRel.dot(up);

        px *= scale / pDepth; py *= scale / pDepth;
        qx *= scale / qDepth; qy *= scale / qDepth;

        return new Point[]{
            new Point((int) px, (int) py),
            new Point((int) qx, (int) qy)
        };
    }

}