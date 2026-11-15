package com.github.arhke.siegeengines.util;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Works out how squarely a projectile hit a surface, by fitting a plane to the blocks around the impact.
 */
public class ImpactAngleUtil {

    private static final int RADIUS = 2;
    private static final int SIZE = RADIUS * 2 + 1;

    /**
     * Gets the factor to scale explosion power by for a projectile hitting a surface.
     *
     * @return 1 for a head-on hit, approaching 0 as the hit gets more glancing
     */
    public static float getImpactFactor(Location impact, Vector velocity) {
        if (velocity.lengthSquared() == 0) {
            return 1f;
        }
        return getImpactFactor(getSurfaceNormal(impact, velocity), velocity);
    }

    /**
     * @param normal the surface normal from {@link #getSurfaceNormal(Location, Vector)}, or null if there isn't one
     * @return 1 for a head-on hit, approaching 0 as the hit gets more glancing
     */
    public static float getImpactFactor(@Nullable Vector normal, Vector velocity) {
        if (normal == null || velocity.lengthSquared() == 0) {
            return 1f;
        }
        return (float) Math.abs(velocity.clone().normalize().dot(normal));
    }

    /**
     * @param normal the surface normal from {@link #getSurfaceNormal(Location, Vector)}
     * @return the normal flipped if needed so it points into the surface, the way the projectile was going
     */
    public static Vector getIntoSurface(Vector normal, Vector velocity) {
        return normal.dot(velocity) < 0 ? normal.clone().multiply(-1) : normal.clone();
    }

    /**
     * Fits a plane to the surface of the solid blocks facing the projectile around the impact.
     *
     * @return the unit normal of the surface, or null if there isn't a surface to fit
     */
    public static @Nullable Vector getSurfaceNormal(Location impact, Vector velocity) {
        final Block center = impact.getBlock();
        final List<int[]> solids = new ArrayList<>();
        final double[] signSum = new double[3];
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int y = -RADIUS; y <= RADIUS; y++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    if (center.getRelative(x, y, z).getType().isSolid()) {
                        solids.add(new int[]{x, y, z});
                        signSum[0] += Integer.signum(x);
                        signSum[1] += Integer.signum(y);
                        signSum[2] += Integer.signum(z);
                    }
                }
            }
        }
        if (solids.isEmpty()) {
            return null;
        }

        // Look at the blocks along the axis pointing most into the wall, from the projectile's side
        final Vector direction = velocity.clone().normalize().multiply(SIZE);
        final double[] view = {signSum[0] + direction.getX(), signSum[1] + direction.getY(), signSum[2] + direction.getZ()};
        int axis = 0;
        for (int i = 1; i < 3; i++) {
            if (Math.abs(view[i]) > Math.abs(view[axis])) {
                axis = i;
            }
        }
        final int sign = view[axis] >= 0 ? 1 : -1;
        final int u = (axis + 1) % 3, v = (axis + 2) % 3;

        // Keep only the block nearest the projectile in each column, which is the visible surface
        final Integer[][] surface = new Integer[SIZE][SIZE];
        for (int[] solid : solids) {
            final int depth = solid[axis] * sign;
            final int column = solid[u] + RADIUS, row = solid[v] + RADIUS;
            if (surface[column][row] == null || depth < surface[column][row]) {
                surface[column][row] = depth;
            }
        }
        final List<double[]> points = new ArrayList<>();
        final double[] centroid = new double[3];
        for (int column = 0; column < SIZE; column++) {
            for (int row = 0; row < SIZE; row++) {
                if (surface[column][row] == null) {
                    continue;
                }
                final double[] point = new double[3];
                point[axis] = surface[column][row] * sign;
                point[u] = column - RADIUS;
                point[v] = row - RADIUS;
                points.add(point);
                for (int i = 0; i < 3; i++) {
                    centroid[i] += point[i];
                }
            }
        }
        if (points.size() < 3) {
            return null;
        }
        for (int i = 0; i < 3; i++) {
            centroid[i] /= points.size();
        }

        // The surface normal is the direction the points vary least in
        final double[][] covariance = new double[3][3];
        for (double[] point : points) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    covariance[i][j] += (point[i] - centroid[i]) * (point[j] - centroid[j]);
                }
            }
        }
        final double[][] eigenvectors = new double[3][3];
        final double[] eigenvalues = eigenDecompose(covariance, eigenvectors);
        int smallest = 0, largest = 0;
        for (int i = 1; i < 3; i++) {
            if (eigenvalues[i] < eigenvalues[smallest]) smallest = i;
            if (eigenvalues[i] > eigenvalues[largest]) largest = i;
        }
        final int middle = 3 - smallest - largest;
        // The points form a line rather than a surface, so there is no single normal
        if (smallest == largest || eigenvalues[middle] < 1e-9) {
            return null;
        }
        return new Vector(eigenvectors[0][smallest], eigenvectors[1][smallest], eigenvectors[2][smallest]).normalize();
    }

    /**
     * Eigen decomposition of a symmetric 3x3 matrix using Jacobi rotations.
     *
     * @param matrix       the symmetric matrix, which is overwritten
     * @param eigenvectors receives the eigenvectors as columns
     * @return the eigenvalues, in the same order as the eigenvector columns
     */
    static double[] eigenDecompose(double[][] matrix, double[][] eigenvectors) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                eigenvectors[i][j] = i == j ? 1 : 0;
            }
        }
        for (int sweep = 0; sweep < 50; sweep++) {
            final double offDiagonal = matrix[0][1] * matrix[0][1] + matrix[0][2] * matrix[0][2] + matrix[1][2] * matrix[1][2];
            if (offDiagonal < 1e-20) {
                break;
            }
            for (int p = 0; p < 2; p++) {
                for (int q = p + 1; q < 3; q++) {
                    rotate(matrix, eigenvectors, p, q);
                }
            }
        }
        return new double[]{matrix[0][0], matrix[1][1], matrix[2][2]};
    }

    private static void rotate(double[][] a, double[][] v, int p, int q) {
        if (Math.abs(a[p][q]) < 1e-20) {
            return;
        }
        final double theta = (a[q][q] - a[p][p]) / (2 * a[p][q]);
        final double t = (theta >= 0 ? 1 : -1) / (Math.abs(theta) + Math.sqrt(theta * theta + 1));
        final double c = 1 / Math.sqrt(t * t + 1), s = t * c;
        // a = J^T a J and v = v J, where J rotates in the p-q plane
        for (int k = 0; k < 3; k++) {
            final double akp = a[k][p], akq = a[k][q];
            a[k][p] = c * akp - s * akq;
            a[k][q] = s * akp + c * akq;
        }
        for (int k = 0; k < 3; k++) {
            final double apk = a[p][k], aqk = a[q][k];
            a[p][k] = c * apk - s * aqk;
            a[q][k] = s * apk + c * aqk;
        }
        for (int k = 0; k < 3; k++) {
            final double vkp = v[k][p], vkq = v[k][q];
            v[k][p] = c * vkp - s * vkq;
            v[k][q] = s * vkp + c * vkq;
        }
    }
}
