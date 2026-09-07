package utils.renderer.astrophys.camera;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
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
import jdk.incubator.vector.VectorSpecies;

public class RayTracer extends Camera{
    private int pixelHeight = 3, pixelWidth = 2;
    public double focalLength;

    private final int THREAD_COUNT = Math.max(Math.min(5, Runtime.getRuntime().availableProcessors()),1);
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

        final int HEIGHT = (int) (environment.RESOLUTION), WIDTH = (int) (HEIGHT * environment.ASPECT_RATIO);
        final Color[][] buf = new Color[WIDTH][HEIGHT];

        final int[] indices = new int[particles.size()];
        for(int i = 0; i<indices.length; i++){
            indices[i] = i;
        }
        final BvhNode node = buildBvhNode(particles, indices, 0, particles.size());

        int columns = WIDTH/pixelWidth, stripeSize = columns/THREAD_COUNT / 2;
        List<Future<?>> futures = new ArrayList<>(THREAD_COUNT/2);

        final Vector stepUp = up.scale(pixelHeight / scale), stepRight = right.scale(pixelWidth / scale);

        for(int n = 0; n<THREAD_COUNT*2; n++){
            final int startCol = n*stripeSize*pixelWidth - WIDTH/2;
            final int endCol = Math.min((n+1)*stripeSize*pixelWidth - WIDTH/2, WIDTH/2);
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

        for(int i = -WIDTH/2; i<WIDTH/2; i+=pixelWidth){
            for(int j = -HEIGHT/2; j<HEIGHT/2; j+=pixelHeight){
                g.setColor(buf[i+WIDTH/2][j+HEIGHT/2]);
                g.fillRect(i, j, pixelWidth, pixelHeight);
            }
        }
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
        Deque<ColorLayer> layers = new ArrayDeque<ColorLayer>();

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

            if(particle.intensity == 0){
                if(layers.size()>0 && layers.peek().operation == ColorLayer.opType.MULTIPLY){
                    layers.peek().compress(particle.color, dist);
                } else{
                    layers.push(new ColorLayer(particle.color, dist));
                }
            } else{
                layers.push(new ColorLayer(particle.color, dist, particle.intensity));
                layers.push(new ColorLayer(particle.color, dist));
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
        while(layers.size()>0){
            layer = layers.pop();
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

    private boolean intersectsBVH(Vector rayOrigin, double invX, double invY, double invZ, BvhNode node){
        double t1 = (node.maxX - rayOrigin.x)*invX,
               t2 = (node.minX - rayOrigin.x)*invX,
               t3 = (node.maxY - rayOrigin.y)*invY,
               t4 = (node.minY - rayOrigin.y)*invY,
               t5 = (node.maxZ - rayOrigin.z)*invZ,
               t6 = (node.minZ - rayOrigin.z)*invZ;
        double tmin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6));
        double tmax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));
        return tmax > 0 && tmin <= tmax;
    }

    private void getIntersection(Vector rayOrigin, Vector rayVec, ArrayList<Particle> particles, int[] indices, BvhNode node, Intersection intersection){
        double invX = 1/rayVec.x, invY = 1/rayVec.y, invZ = 1/rayVec.z;
        double a = rayVec.dot(rayVec);
        Particle p; Vector diff = new Vector(Vector.ORIGIN);

        BvhNode[] stack = new BvhNode[64];
        int head = 0;
        stack[head++] = node;

        while(head>0){
            BvhNode current = stack[--head];
            if(current.isLeaf){
                for(int i = current.start; i<current.start + current.count; i++){
                    p = particles.get(indices[i]);
                    diff.copy(rayOrigin).subtractInPlace(p.pos);

                    double b = 2*diff.dot(rayVec);
                    double c = diff.dot(diff) - p.rad*p.rad;

                    double discriminant = b*b - 4*a*c;

                    if(discriminant < 0) continue;
                    double dist = (-b - Math.sqrt(discriminant))/(2*a);
                    if(dist < 1e-9) dist = -dist - b/a;

                    if(dist < 1e-9) continue;
                    if(dist > intersection.intersectDist) continue;
                    intersection.intersectParticle = p;
                    intersection.intersectDist = dist;
                    intersection.intersectPoint = rayOrigin.add(rayVec.scale(dist));
                }
                continue;
            }

            if(intersectsBVH(rayOrigin, invX, invY, invZ, current.right))
                stack[head++] = current.right;
            if(intersectsBVH(rayOrigin, invX, invY, invZ, current.left))
                stack[head++] = current.left;
        }
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