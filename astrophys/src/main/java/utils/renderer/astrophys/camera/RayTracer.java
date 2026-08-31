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
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
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

        int height = (int) (environment.RESOLUTION), width = (int) (height * environment.ASPECT_RATIO);
        Color[][] buf = new Color[width][height];

        List<Future<?>> futures = new ArrayList<>((int) (width / pixelWidth) + 1);
        for(int i = -width/2; i<width/2; i+=pixelWidth){
            final int ii = i;
            futures.add(executor.submit(() -> {
                for(int j = -height/2; j<height/2; j+=pixelHeight){
                    Vector pixel = pos.add(right.scale(ii+pixelWidth/2).addInPlace(up.scale(j+pixelHeight/2)).scaleInPlace(1/scale));
                    buf[ii+width/2][j+height/2] = getColor(focus, pixel, particles);
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

        for(int i = -width/2; i<width/2; i+=pixelWidth){
            for(int j = -height/2; j<height/2; j+=pixelHeight){
                g.setColor(buf[i+width/2][j+height/2]);
                g.fillRect(i, j, pixelWidth, pixelHeight);
            }
        }
    }

    private Color getColor(Vector focus, Vector pixel, ArrayList<Particle> particles){
        Vector origin = pixel;
        Vector ray = pixel.subtract(focus);

        int reflections = 0, maxReflections = !useLighting ? 1 : 30;
        Deque<ColorLayer> layers = new ArrayDeque<ColorLayer>();
        while(reflections<maxReflections){
            Intersection intersection = getIntersection(origin, ray, particles);
            Particle particle = intersection.intersectParticle;
            Vector newOrigin = intersection.intersectPoint;
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

            Vector incoming1 = ray.scale(-1).normalize();
            Vector normal = particle.pos.subtract(newOrigin).normalize();
            ray = incoming1.add(normal.scale(2*incoming1.dot(normal))).normalize();
            origin = newOrigin;

            reflections++;
        }

        BigColor color = new BigColor(environment.BACKGROUND_COLOR);
        color.scale(1e100);

        while(layers.size()>0){
            ColorLayer layer = layers.pop();
            if(layer.operation == ColorLayer.opType.ADD){
                color.add(layer.color, layer.intensity);
                color.scale(layer.dropoff);
            } else{
                color.multiply(layer.color, layer.dropoff);
            }
        }
        return color.normalize();
    }

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
    
    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;

    private Intersection getIntersection(Vector rayOrigin, Vector rayVec, ArrayList<Particle> particles){
        rayVec.normalizeInPlace();
        Particle intersectParticle = null;
        int intersectIdx = -1;
        double intersectDist = Double.POSITIVE_INFINITY;

        // Preparing particles for SIMD (all vectors use Upper-lowercase naming convention.)
        double[] px = new double[particles.size()];
        double[] py = new double[particles.size()];
        double[] pz = new double[particles.size()];
        double[] rad = new double[particles.size()];
        for(int i = 0; i<particles.size(); i++){
            Particle p = particles.get(i);
            px[i] = p.pos.x; py[i] = p.pos.y; pz[i] = p.pos.z; rad[i] = p.rad;
        }
        
        DoubleVector Ox = DoubleVector.broadcast(SPECIES, rayOrigin.x);
        DoubleVector Oy = DoubleVector.broadcast(SPECIES, rayOrigin.y);
        DoubleVector Oz = DoubleVector.broadcast(SPECIES, rayOrigin.z);

        DoubleVector Rx = DoubleVector.broadcast(SPECIES, rayVec.x);
        DoubleVector Ry = DoubleVector.broadcast(SPECIES, rayVec.y);
        DoubleVector Rz = DoubleVector.broadcast(SPECIES, rayVec.z);

        DoubleVector A = DoubleVector.broadcast(SPECIES, rayVec.dot(rayVec));

        for(int i = 0; i<particles.size(); i+=SPECIES.length()){
            var mask = SPECIES.indexInRange(i, particles.size());

            DoubleVector Px = DoubleVector.fromArray(SPECIES, px, i, mask);
            DoubleVector Py = DoubleVector.fromArray(SPECIES, py, i, mask);
            DoubleVector Pz = DoubleVector.fromArray(SPECIES, pz, i, mask);
            DoubleVector Rad = DoubleVector.fromArray(SPECIES, rad, i, mask);

            DoubleVector Dx = Ox.sub(Px), Dy = Oy.sub(Py), Dz = Oz.sub(Pz);
            DoubleVector B = Dx.mul(Rx).add(Dy.mul(Ry)).add(Dz.mul(Rz)).mul(2);
            DoubleVector C = Dx.mul(Dx).add(Dy.mul(Dy)).add(Dz.mul(Dz)).sub(Rad.mul(Rad));

            DoubleVector Disc = B.mul(B).sub(A.mul(C).mul(4));
            
            var PositiveDiscMask = Disc.compare(VectorOperators.GT, 0);
            if(!PositiveDiscMask.anyTrue()) continue;

            DoubleVector Dist = B.add(Disc.lanewise(VectorOperators.SQRT, PositiveDiscMask)).mul(-1).div(A.mul(2));
            DoubleVector AltDist = Dist.add(B.div(A)).mul(-1);

            var SmallDistMask = Dist.compare(VectorOperators.LT, 1e-9);
            Dist = Dist.blend(AltDist, SmallDistMask);

            var ValidMask = Dist.compare(VectorOperators.GT, 1e-9);

            double min = Dist.reduceLanes(VectorOperators.MIN, ValidMask);
            var MinMask = Dist.compare(VectorOperators.EQ, min);
            int lane = MinMask.firstTrue();

            if(min < intersectDist){
                intersectDist = min; intersectIdx = i+lane;
            }
        }

        // for(Particle p : particles){
        //     Vector diff = rayOrigin.subtract(p.pos);
        //     double b = 2*diff.dot(rayVec);
        //     double c = diff.dot(diff) - p.rad*p.rad;

        //     double discriminant = b*b - 4*a*c;

        //     if(discriminant < 0) continue;
        //     double dist = (-b - Math.sqrt(discriminant))/(2*a);
        //     if(dist < 1e-9) dist = -dist - b/a;

        //     if(dist < 1e-9 || dist > intersectDist) continue;
        //     intersectParticle = p; intersectDist = dist;
        // }
        intersectParticle = intersectIdx > -1 ? particles.get(intersectIdx) : null;
        Vector intersectVec = rayOrigin.add(rayVec.scale(intersectDist));
        return new Intersection(intersectParticle, intersectVec, intersectDist);
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