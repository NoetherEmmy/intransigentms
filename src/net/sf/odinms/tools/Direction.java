package net.sf.odinms.tools;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

public enum Direction {
                  UP  (new Vect(0,  1)),
    LEFT (new Vect(-1, 0)), RIGHT (new Vect(1, 0)),
                 DOWN (new Vect(0, -1));

    private final Vect unitVect;
    private static final Map<Vect, Direction> map =
                     stream(Direction.values())
                    .collect(toMap(od -> od.unitVect, od -> od));

    Direction(Vect unitVect) {
        this.unitVect = unitVect;
    }

    public static Direction valueOf(IVect direction) {
        return map.get(direction.unit().asVect());
    }
    
    public Vect unitVect() {
        return unitVect;
    }
    
    public static Set<Direction> directionsOf(final IVect v) {
        return map.entrySet()
                  .stream()
                  .filter(e -> !v.directionalProj(e.getKey()).isZero())
                  .map(Map.Entry::getValue)
                  .collect(Collectors.toSet());
    }
}
