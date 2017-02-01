package net.sf.odinms.tools;

import java.util.ArrayList;
import java.util.List;

public interface IVect {
    Vect asVect();

    VectR asVectR();

    Number x(int index);

    double realX(int index);

    List<? extends Number> xs();

    List<Double> realXs();

    void let(int index, int x);

    void let(int index, double x);

    int dim();

    IVect add(IVect v);

    IVect subtract(IVect v);

    IVect scalarMult(final int scalar);

    IVect scalarMult(final double scalar);

    IVect scalarAdd(final int scalar);

    IVect scalarAdd(final double scalar);

    IVect scalarAdd(int index, int scalar);

    IVect scalarAdd(int index, double scalar);

    double norm();

    double dot(IVect v);

    IVect cross(IVect v);

    IVect unit();

    IVect basisVect();

    List<? extends IVect> decomp();

    double scalar3Prod(IVect v, IVect u);

    IVect vect3Prod(IVect v, IVect u);

    double wedge(IVect v);

    double angle(IVect v);

    boolean isZero();

    IVect proj(IVect v);

    IVect directionalProj(IVect v);

    boolean equals(Object o);

    String toString();

    /** Returns a(n ordered) list (<code>ArrayList</code>) of <code>Vect</code>s representing the basis vectors for the given
     ** <code>dimension</code> */
    static List<Vect> basis(int dimension) {
        List<Vect> basis = new ArrayList<>(dimension);
        for (int i = 0; i < dimension; ++i) {
            Vect v = new Vect(dimension);
            v.let(i, 1);
            basis.add(v);
        }
        return basis;
    }

    /** Returns a(n ordered) list (<code>ArrayList</code>) of <code>VectR</code>s representing the basis vectors for the given
     ** <code>dimension</code> */
    static List<VectR> basisR(int dimension) {
        List<VectR> basis = new ArrayList<>(dimension);
        for (int i = 0; i < dimension; ++i) {
            VectR v = new VectR(dimension);
            v.let(i, 1);
            basis.add(v);
        }
        return basis;
    }
}
