package net.sf.odinms.tools;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class VectR implements IVect {
    private final List<Double> elems;

    /** Initializes an {@code n}-dimensional 0-vector (all elements set
     ** equal to zero).
     **
     ** @throws IllegalArgumentException when {@code n < 2} */
    public VectR(final int n) {
        this(new ArrayList<>(Collections.nCopies(n, 0.0d)));
    }

    /** Copy constructor.
     **
     ** @throws NullPointerException when {@code elems == null} */
    public VectR(final VectR v) {
        this(v.xs());
    }

    /** Initializes a vector using each member of the
     ** specified collection, in order, as an element.
     **
     ** @throws IllegalArgumentException when {@code xs.size() < 2}
     **
     ** @throws NullPointerException when {@code xs == null} */
    public VectR(final Collection<Double> xs) {
        elems = new ArrayList<>(xs);
        if (elems.size() < 2) {
            throw new IllegalArgumentException("Vect must be of at least dimension 2.");
        }
    }

    /** Initializes a vector using the integer elements of {@code xs},
     ** in order, as the elements of the vector.
     **
     ** @throws IllegalArgumentException when {@code xs.length < 2} */
    public VectR(final double... xs) {
        elems = new ArrayList<>(xs.length);
        if (xs.length < 2) {
            throw new IllegalArgumentException("Vect must be of at least dimension 2.");
        }
        for (final double x : xs) {
            elems.add(x);
        }
    }

    /** Takes in a <code>Point</code>, constructing a new <code>VectR</code> using
     ** the given <code>Point</code>'s coordinates in 2-space.
     **
     ** @throws NullPointerException when {@code p == null} */
    public VectR(final Point p) {
        this(p.getX(), p.getY());
    }

    /** Takes in a <code>Point</code>, constructing a new <code>VectR</code> using
     ** the given <code>Point</code>'s coordinates in 2-space.
     **
     ** @throws NullPointerException when {@code p == null} */
    public static VectR point(final Point p) {
        return new VectR(p.getX(), p.getY());
    }

    /** Returns this <code>IVect</code> object's equivalent as a <code>Vect</code> */
    @Override
    public Vect asVect() {
        return new Vect(elems.stream().map(e -> (int) Math.round(e)).collect(Collectors.toList()));
    }

    /** Returns this <code>IVect</code> object's equivalent as a <code>VectR</code> */
    @Override
    public VectR asVectR() {
        return this;
    }

    /** Returns the element of the vector at the 0-based position
     ** specified by {@code index}
     **
     ** @throws IndexOutOfBoundsException if the vector has no such index
     **        ({@code index < 0 || index >= dim()}) */
    @Override
    public Double x(final int index) {
        return elems.get(index);
    }

    /** Returns the element of the vector at the 0-based position
     ** specified by {@code index} as a {@code double} (real number).
     **
     ** @throws IndexOutOfBoundsException if the vector has no such index
     **        ({@code index < 0 || index >= dim()}) */
    @Override
    public double realX(final int index) {
        return elems.get(index);
    }

    /** Returns an <b>unmodifiable</b> view of the <code>Integer</code> elements of the vector,
     ** as a list. */
    @Override
    public List<Double> xs() {
        return Collections.unmodifiableList(elems);
    }

    /** Returns an <b>new</b> view of the <code>Integer</code> elements of the vector,
     ** converted to a list of <code>Double</code>s. */
    @Override
    public List<Double> realXs() {
        return Collections.unmodifiableList(elems);
    }

    /** Sets the value of the element specified by {@code index}
     ** to {@code x}
     **
     ** @throws IndexOutOfBoundsException if the vector has no such index
     **        ({@code index < 0 || index >= dim()}) */
    @Override
    public void let(final int index, final int x) {
        elems.set(index, (double) x);
    }

    /** Sets the value of the element specified by {@code index}
     ** to {@code x}
     **
     ** @throws IndexOutOfBoundsException if the vector has no such index
     **        ({@code index < 0 || index >= dim()}) */
    @Override
    public void let(final int index, final double x) {
        elems.set(index, x);
    }

    /** Returns the dimension of the vector. */
    @Override
    public int dim() {
        return elems.size();
    }

    /** Adds the specified vector {@code v} to this vector and returns
     ** the resulting vector.
     **
     ** @throws IllegalArgumentException when {@code this.dim() != v.dim()}
     **
     ** @throws NullPointerException when {@code v == null} */
    @Override
    public VectR add(final IVect v) {
        if (dim() != v.dim()) {
            throw new IllegalArgumentException("Cannot add vectors of non-matching dimensions.");
        }
        final List<Double> sum = new ArrayList<>(dim());
        for (int i = 0; i < dim(); ++i) {
            sum.add(x(i) + v.x(i).doubleValue());
        }
        return new VectR(sum);
    }

    /** Subtracts the specified vector {@code v} from this vector and returns
     ** the resulting vector.
     **
     ** @throws IllegalArgumentException when {@code this.dim() != v.dim()}
     **
     ** @throws NullPointerException when {@code v == null} */
    @Override
    public VectR subtract(final IVect v) {
        if (dim() != v.dim()) {
            throw new IllegalArgumentException("Cannot subtract vectors of mismatched dimensions.");
        }
        final List<Double> difference = new ArrayList<>(dim());
        for (int i = 0; i < dim(); ++i) {
            difference.add(x(i) - v.x(i).doubleValue());
        }
        return new VectR(difference);
    }

    /** Multiplies this vector by the given {@code scalar} and returns the resulting vector. */
    @Override
    public VectR scalarMult(final int scalar) {
        return new VectR(elems.stream().map(e -> e * scalar).collect(Collectors.toList()));
    }

    /** Multiplies this vector by the given {@code scalar}
     ** and returns the resulting vector. */
    @Override
    public VectR scalarMult(final double scalar) {
        return new VectR(elems.stream().map(e -> e * scalar).collect(Collectors.toList()));
    }

    /** Adds {@code scalar} to every element of this vector and returns the resulting vector.
     ** {@code scalar} can be negative. */
    @Override
    public VectR scalarAdd(final int scalar) {
        return new VectR(elems.stream().map(e -> e + scalar).collect(Collectors.toList()));
    }

    /** Adds {@code scalar} to every element of this vector
     ** and returns the resulting vector.
     ** {@code scalar} can be negative. */
    @Override
    public VectR scalarAdd(final double scalar) {
        return new VectR(elems.stream().map(e -> e + scalar).collect(Collectors.toList()));
    }

    /** Adds {@code scalar} to the element of this vector specified by {@code index}
     ** and returns the resulting vector.
     ** {@code scalar} can be negative.
     **
     ** @throws IndexOutOfBoundsException when {@code index >= dim()} */
    @Override
    public VectR scalarAdd(final int index, final int scalar) {
        final VectR v = new VectR(this);
        v.let(index, v.x(index) + scalar);
        return v;
    }

    /** Adds {@code scalar} to the element of this vector specified by {@code index}
     ** and returns the resulting vector.
     ** {@code scalar} can be negative.
     **
     ** @throws IndexOutOfBoundsException when {@code index >= dim()} */
    @Override
    public VectR scalarAdd(final int index, final double scalar) {
        final VectR v = new VectR(this);
        v.let(index, v.x(index) + scalar);
        return v;
    }

    /** Returns the norm (a.k.a. modulus or magnitude) of this vector as a {@code double} */
    @Override
    public double norm() {
        return Math.sqrt(elems.stream().reduce(0.0d, (s, e) -> s + Math.pow(e, 2)));
    }

    /** Takes the dot product of the given vector {@code v} with the vector,
     ** and returns the resulting scalar as a {@code double}
     **
     ** @throws IllegalArgumentException when {@code this.dim() != v.dim()}
     **
     ** @throws NullPointerException when {@code v == null} */
    @Override
    public double dot(final IVect v) {
        if (dim() != v.dim()) {
            throw new IllegalArgumentException("Cannot dot product vectors of mismatched dimensions.");
        }
        double product = 0.0d;
        for (int i = 0; i < dim(); ++i) {
            product += realX(i) * v.realX(i);
        }
        return product;
    }

    /** <p>Takes the cross product of two 3-vectors or two 7-vectors, returning the resulting vector
     ** (strictly speaking, a pseudo-vector) in the same space.</p>
     **
     ** <p>The cross product in 3-space is guaranteed to be unique, but the cross product in
     ** 7-space is <b>not</b> unique; the table used here is a common one given by Cayley. However,
     ** the cross product in 7-space does still retain all of the algebraic properties of its
     ** 3-space cousin. For generalization to n-space, use <code>wedge<code/></p>
     **
     ** @throws UnsupportedOperationException when {@code dim() != 3 && dim() != 7}
     **
     ** @throws IllegalArgumentException when {@code dim() != v.dim()}
     **
     ** @throws NullPointerException when {@code v == null} */
    @Override
    public VectR cross(final IVect v) {
        if (dim() != 3 && dim() != 7) {
            throw new UnsupportedOperationException("Can only perform cross product in 3- or 7-space.");
        }
        if (dim() != v.dim()) {
            throw new IllegalArgumentException("Cannot cross vectors of mismatched dimensions.");
        }
        final VectR cross = new VectR(dim());
        if (dim() == 3) {
            final int[][] multiTable =
                {{0, 3, -2},
                 {-3, 0, 1},
                 {2, -1, 0}};
            for (int i = 0; i < multiTable.length; ++i) {
                for (int j = 0; j < multiTable[i].length; ++j) {
                    if (multiTable[i][j] != 0) {
                        cross.let(Math.abs(multiTable[i][j]) - 1, cross.x(Math.abs(multiTable[i][j]) - 1) +
                                x(i) * v.x(j).doubleValue() * Integer.signum(multiTable[i][j]));
                    }
                }
            }
        } else {
            final int[][] multiTable =
                {{0, 3, -2, 5, -4, -7, 6},
                 {-3, 0, 1, 6, 7, -4, -5},
                 {2, -1, 0, 7, -6, 5, -4},
                 {-5, -6, -7, 0, 1, 2, 3},
                 {4, -7, 6, -1, 0, -3, 2},
                 {7, 4, -5, -2, 3, 0, -1},
                 {-6, 5, 4, -3, -2, 1, 0}};
            for (int i = 0; i < multiTable.length; ++i) {
                for (int j = 0; j < multiTable[i].length; ++j) {
                    if (multiTable[i][j] != 0) {
                        cross.let(Math.abs(multiTable[i][j]) - 1, cross.x(Math.abs(multiTable[i][j]) - 1) +
                                x(i) * v.x(j).doubleValue() * Integer.signum(multiTable[i][j]));
                    }
                }
            }
        }
        return cross;
    }

    /** Returns the unit vector that is parallel (not anti-parallel) to this vector. */
    @Override
    public VectR unit() {
        final double norm = norm();
        return new VectR(elems.stream().map(e -> e / norm).collect(Collectors.toList()));
    }

    /** Returns the basis vector that is closest to being parallel to this vector.
     ** If there is a tie, the basis vector with the lowest index wins. If this vector
     ** is a zero-vector, returns the zero-vector, otherwise guaranteed to return a
     ** unit basis vector. */
    @Override
    public VectR basisVect() {
        final VectR basisV = new VectR(dim());
        if (isZero()) return basisV;
        int maxIndex = 0;
        for (int i = 1; i < dim(); ++i) {
            if (elems.get(i) > elems.get(maxIndex)) {
                maxIndex = i;
            }
        }
        basisV.let(maxIndex, 1);
        return basisV;
    }

    /** Decomposes this vector into a(n ordered) list of vectors representing the magnitude
     ** of this vector along each axis (i.e. projected along each basis vector), and returns
     ** the list (<code>ArrayList</code>). */
    @Override
    public List<VectR> decomp() {
        final List<VectR> decomp = new ArrayList<>(dim());
        for (int i = 0; i < dim(); ++i) {
            final VectR v = new VectR(dim());
            v.let(i, elems.get(i));
            decomp.add(v);
        }
        return decomp;
    }

    /** Takes the scalar triple product of this vector with the two specified vectors {@code v}
     ** and {@code u}, as follows:
     ** <pre>
     **     {@code this} &#8901;{@code (v} &#10799;{@code u)}</pre>
     **
     ** Or, in code,
     ** <pre>
     **     {@code this.dot(v.cross(u))}</pre>
     **
     ** The scalar is returned as a {@code double}
     **
     ** @throws UnsupportedOperationException when {@code v.dim() != 3 && v.dim() != 7}
     **
     ** @throws IllegalArgumentException any time that the dimensions of
     ** {@code this}, {@code v}, and {@code u} don't all exactly match.
     **
     ** @throws NullPointerException when {@code v == null || u == null} */
    @Override
    public double scalar3Prod(final IVect v, final IVect u) {
        return dot(v.cross(u));
    }

    /** Takes the vector triple product of this vector with the two specified vectors {@code v}
     ** and {@code u}, as follows:
     ** <pre>
     **     {@code this} &#10799;{@code (v} &#10799;{@code u)}</pre>
     **
     ** Or, in code,
     ** <pre>
     **     {@code this.cross(v.cross(u))}</pre>
     **
     ** The new resulting vector is then returned.
     **
     ** @throws UnsupportedOperationException when {@code (dim() != 3 && dim() != 7) || (v.dim() != 3 && v.dim() != 7)}
     **
     ** @throws IllegalArgumentException any time that the dimensions of
     ** {@code this}, {@code v}, and {@code u} don't all exactly match.
     **
     ** @throws NullPointerException when {@code v == null || u == null} */
    @Override
    public VectR vect3Prod(final IVect v, final IVect u) {
        return cross(v.cross(u));
    }

    /**
     * Returns the <b>unsigned magnitude</b> of the wedge product of this vector and the specified vector {@code v},
     * equal to:
     * <pre>
     *     ||<code>this</code>|| ||<code>v</code>|| sin(&#952;)</pre>
     * Where &#952; is the shortest angle between <code>this</code> and <code>v</code>. Or, in code,
     * <pre>
     *    {@code this.norm() * v.norm() * Math.sin(this.angle(v))}</pre>
     * Normally (mathematically), the wedge product returns a <i>bivector</i>, not a scalar. This returns
     * the magnitude of that bivector, as a generalization of the magnitude of the cross product to n-dimensional
     * space.
     *
     * @throws IllegalArgumentException when {@code this.dim() != v.dim()}
     *
     * @throws NullPointerException when {@code v == null}
     */
    @Override
    public double wedge(final IVect v) {
        if (dim() != v.dim()) {
            throw new IllegalArgumentException("Cannot wedge vectors of mismatched dimensions.");
        }
        return norm() * v.norm() * Math.sin(angle(v));
    }

    /**
     * <p>Returns the shortest angle between this vector and the given vector {@code v}</p>
     * <p>Value is in the range from 0.0 through &#960; radians.</p>
     *
     * @throws IllegalArgumentException when {@code this.dim() != v.dim()}
     *
     * @throws NullPointerException when {@code v == null}
     */
    @Override
    public double angle(final IVect v) {
        if (dim() != v.dim()) {
            throw new IllegalArgumentException("Cannot wedge vectors of mismatched dimensions.");
        }
        return Math.acos(dot(v) / (norm() * v.norm()));
    }

    /** Returns {@code true} iff all elements of this vector are zero. */
    public boolean isZero() {
        return elems.stream().allMatch(e -> e.equals(0.0d));
    }

    /**
     * Performs the projection of this vector onto the specified vector {@code v},
     * and then returns the new resulting vector.
     *
     * @throws IllegalArgumentException when {@code this.dim() != v.dim()}
     *
     * @throws NullPointerException when {@code v == null}
     */
    @Override
    public VectR proj(final IVect v) {
        if (dim() != v.dim()) {
            throw new IllegalArgumentException("Cannot project vectors of mismatched dimensions.");
        }
        if (v.isZero()) {
            return new VectR(dim());
        }
        final VectR _v = v.asVectR();
        return _v.scalarMult(dot(_v) / _v.dot(_v));
    }

    /**
     * Performs the <b>directional</b> projection of this vector onto the specified vector {@code v},
     * and then returns the new resulting vector. The directional projection acts like a normal
     * projection, except that it returns the zero-vector any time the projection is <i>anti</i> -parallel
     * to the vector being projected onto (in this case, <code>v</code>).
     *
     * @throws IllegalArgumentException when {@code this.dim() != v.dim()}
     *
     * @throws NullPointerException when {@code v == null}
     */
    @Override
    public VectR directionalProj(final IVect v) {
        if (dim() != v.dim()) {
            throw new IllegalArgumentException("Cannot project vectors of mismatched dimensions.");
        }
        if (norm() == 0.0d || v.norm() == 0.0d || dot(v) / (norm() * v.norm()) < 0.0d) {
            return new VectR(dim());
        }
        return proj(v);
    }

    /**
     * <p>Determines whether or not two vectors are equal.</p>
     * <p>If the vectors being compared are dissimilar types ({@code Vect} and {@code VectR}),
     * then the <code>.doubleValue()</code>s of the {@code Vect}'s elements must
     * <code>.equals()</code> the values of the {@code VectR}'s elements for this to
     * return <code>true</code></p>
     */
    @Override
    public boolean equals(final Object o) {
        if (o instanceof IVect) {
            final VectR v = ((IVect) o).asVectR();
            if (dim() != v.dim()) return false;
            for (int i = 0; i < dim(); ++i) {
                if (!x(i).equals(v.x(i))) return false;
            }
            return true;
        }
        return false;
    }

    /** Returns a {@code String} that represents the vector. */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append(" <");
        for (int i = 0; i < elems.size() - 1; ++i) {
            sb.append(elems.get(i)).append(", ");
        }
        sb.append(elems.get(elems.size() - 1)).append('>');
        return sb.toString();
    }
}
