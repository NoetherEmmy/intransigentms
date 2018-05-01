# Coding guidelines

## Style

+ Do not use tabs. When indenting, use four spaces per level of indentation.
+ Use indentation appropriately and consistently, as shown throughout the examples.
+ Lines should not be longer than 120 characters (ideally no longer than 79).
    - Ways to produce shorter lines:
        * Chained methods almost always look better when each method call is on its own line, in line with other methods in the chain:

            ```java
            final List<Integer> validIds =
                inputStrings.stream()
                            .filter(Objects::nonNull)
                            .map(s -> {
                                try {
                                    return Optional.of(Integer.parseInt(s));
                                } catch (NumberFormatException nfe) {
                                    return Optional.empty();
                                }
                            })
                            .flatMap(Optional::stream)
                            .sorted()
                            .collect(Collectors.toCollection(ArrayList::new));
            ```

        * Give arguments their own lines:

            ```java
            final var toSend =
                MaplePacketCreator.damagePlayer(
                    isSkill() ? skillId : -1,
                    attacker.getId(),
                    player.getId(),
                    damageInstances.get(0),
                    isFake() ? fake : 0,
                    direction,
                    false,
                    0,
                    true,
                    attacker.getObjectId(),
                    attacker.getPosition().x,
                    attacker.getPosition().y
                );
            ```

        * Put newlines after infix operators and indent appropriately:

            ```java
            final var longString =
                "This is a really long string. This is a really " +
                "long string. This is a really long string. This" +
                " is a really long string. This is a really long" +
                " string. This is a really long string.";
            ```

        * Split out large expressions into multiple local variables and compose them later.
+ Comment your code.
+ Write javadoc comments directly above all new methods that you create unless the method is particularly obvious (e.g. a getter/setter):
    - Javadoc comments are of this form (**NOTE** the `/**` as opposed to `/*`):

        ```java
        /**
         * `Description of what the method does`
         * `some more description...`
         * `etc...`
         *
         * <ul>
         * <li>pure?: `true/false`</li>
         * <li>nullable?: `true/false`</li>
         * </ul>
         *
         * @param `parameter name` `parameter description`
         * @param `parameter name` `parameter description`
         * @param...
         *
         * @return `description of return value and what values it can take`
         *
         * @throws `exception name` when `condition`
         * @throws `exception name` when `condition`
         * @throws...
         */
        ```

    - Of course, the `@param` parts of the javadoc can be omitted if the method takes no parameters, `@return` can be omitted if the method returns `void`, `@throws` can be omitted if the method has no notable exceptions it's prone to throw, and `nullable?` can be omitted if the return type is `void` or the return type is a type that isn't `null`able (e.g. `int`, `boolean`, and even `Optional<T>` in this case).
    - `pure?` refers to whether or not the method is a [pure function](https://en.wikipedia.org/wiki/Pure_function).

+ Block comments should be of the form:

    ```java
    /*
     * This is a nice block comment.
     * It's easy to read.
     * It's pretty.
     */
    ```

+ Single line comments (`//`) should either be on their own line, or be separated from the code that they trail by one or two spaces. In addition, there should be exactly one space between the `//` and the contents of the comment, unless the comment is simply disabling ("commenting out") code, in which case there should be no space between `//` and the disabled code.
+ Use plain english in comments (interspersed with code terms as necessary), with capitalization and punctuation.
+ All source files must end with a trailing newline.
+ All field declarations should be at the top of a class definition.
+ There should be a blank line between field declarations and the first method declaration.
+ There should be blank lines between all method declarations.
+ Class/type names should have camel case for all words, including initialisms:
    - Bad:
        * `MAPLE_PARTY_CHARACTER`
        * `maplePartyCharacter`
        * `maple_party_character`
        * `NPCScriptValidator`
    - Good:
        * `MaplePartyCharacter`
        * `NpcScriptValidator`
+ Local variables, method parameters, fields, and methods should all have camel case names with the first letter lowercase, including initialisms:
    - Bad:

        ````java
        private void toggle_obstacles_with_id(boolean ReactorId) {
            int temporaryvariable;
            // ...
        }
        ```

    - Good:

        ```java
        private void toggleObstaclesWithId(boolean reactorId) {
            int temporaryVariable;
            // ...
        }
        ```

+ Enum values and constants should be in shouting snake case:
    - Bad:

        ```java
        public static final double maceProp = 0.012d;
        ```

    - Good:

        ```java
        public static final double MACE_PROP = 0.012d;
        ```

+ When `catch`ing an exception, the name of the caught exception should be an all-lowercase initialism of the type of the exception (e.g. npe for `NullPointerException` or sqle for `SQLException`), unless you have a more informative name in mind.
+ When `catch`ing an exception and ignoring it (which should only be done in very specific cases and should almost never be done with `Throwable` or `Exception`; catch specific exceptions instead), name the caught exception `ignored` and put the closing brace and the immediate next line:

    - Bad:

        ```java
        try {
            theString = Integer.parseInt(n);
        } catch (Exception e) {

        }
        ```

    - Good:

        ```java
        try {
            theString = Integer.parseInt(n);
        } catch (NumberFormatException ignored) {
        }
        ```

+ When creating a new class for a specific purpose and that purpose is only really used extensively by one class in particular (i.e. it can be encapsulated by that class), prefer to create a nested class (class definition inside of class definition) over putting the class in its own file.
+ Never start the contents of a pair of curly braces (`{}`) with a blank line, including method and class definitions:
    - Bad:

        ```java
        class Foo implements Bar {

            private int baz;

            public Foo(int baz) {
                this.baz = baz;
            }
        }
        ```

    - Good:

        ```java
        class Foo implements Bar {
            private int baz;

            public Foo(int baz) {
                this.baz = baz;
            }
        }
        ```

+ All floating-point literals should have their types annotated with a lowercase letter:

    ```java
    float myFloat = 0.35f;
    double myDouble = 11.49101d;
    ```

+ `long` literals should have their types annotated with an uppercase L:

    ```java
    long myLong = 51398010673L;
    ```

+ All floating-point literals should have at least one decimal digit before the decimal point, even when that digit is `0`. In addition, there should not be trailing zeroes:
    - Bad:

        ```java
        double myDouble = .32147890d;
        ```

    - Good:

        ```java
        double myDouble = 0.3214789d;
        ```

+ Use diamond inference whenever possible:
    - Bad:

        ```java
        List<String> someStrings = new ArrayList<String>();
        ```

    - Good:

        ```java
        List<String> someStrings = new ArrayList<>();
        ```

    - Also good:

        ```java
        var someStrings = new ArrayList<String>();
        ```

+ Whenever there exist syntactic tokens separated by commas (e.g. argument lists, array literals), put a single space between each comma and the following item.
+ Array literals (and other possibly similar syntax) should be arranged like the following when it matters for line length/readability:

    ```java
    final int[] someInts =
    {
        415, 5,   837, 461, 101, 694, 147, 3,   81,  919,
        555, 319, 79,  597, 66,  461, 267, 567, 6,   191,
        352, 353, 444, 537, 859, 0,   97,  444, 231, 296,
        1,   987, 202, 51
    };
    ```

+ Whenever possible, use lambda (`->`) syntax. Do **not** explicitly create a new instance of `Runnable` and `@Override` its `run()` method.
+ Lambda expressions should be styled with the following rules:
    - Use method references instead of using explicit arrow syntax whenever possible:
        * Bad:

            ```java
            final var myInts =
                strings.stream()
                       .filter(s -> s != null && s.length() > 0)
                       .map(i -> Integer.parseInt(i))
                       .collect(Collectors.toSet());
            ```

        * Good:

            ```java
            final var myInts =
                strings.stream()
                       .filter(s -> s != null && s.length() > 0)
                       .map(Integer::parseInt)
                       .collect(Collectors.toSet());
            ```

    - Do not put parentheses around the arguments list of a lambda if there is only one argument.
    - Do not put curly braces around the body of a lambda if the function is a single statement.
    - When putting a line break after the arguments list of a lambda, do so immediately after the `->` if there is only one statement, and immediately after the `{` otherwise.
+ Do not use the `this` keyword to refer to fields and methods unless necessary, e.g. the name of the field/method has been "shadowed" by a local variable or when using `::` notation.
+ Use the `sort` method of the `Collection` object that is being sorted when doing an in-place sort, as opposed to using the static `Collections.sort()` method.
+ `if`, `else if`, `else`, `while`, `for`, `synchronized`, `using`, `try`, and `catch` blocks should follow the following style:
    - They must be enclosed with curly braces (`{}`), with one exception:
        * When there is a stand-alone `if` statement whose condition and body statements are quite short, it can be written all on one line without line breaks.
    - There should be a single space after the keyword, a single space after the parentheses, a newline after the opening curly brace, and a single newline between the end of the last statement and the closing curly brace:
        * Bad:

            ```java
            if(condition)
            {
                foo = new Foo();
            } else { foo = null; }
            ```

        * Good:

            ```java
            if (condition) {
                foo = new Foo();
            } else {
                foo = null;
            }
            ```

+ Method declarations should be of the following form:

    ```java
    /**
     * Javadoc
     * goes
     * here
     */
    private List<Foo> generateFoos(int fooCount) {
        // ...
        return someFoos;
    }
    ```

+ Always use the `@Override` annotation at the top of a method when it overrides another method.

## Method

+ Avoid using explicit loops (`for`, `while`) and temporary variables (e.g. `int i = 0;`, `List<Object> objectsCollectedInLoop = new ArrayList<>();`). Prefer to use Java 8 features like `.forEach()` and the `Stream` interface (`.map()`, `.filter()`, `.reduce()`, etc.) instead, unless explicit loops are necessary or `Stream` methods are problematic in the specific case in question.
+ When one has found the need to use explicit loops, use `:` syntax whenever possible:

    ```java
    for (final var chr : characterList) {
        // Do shit
    }
    ```

+ When using the `Stream` inferface, avoid inadvertently dereferencing a `null` reference when appropriate like so:

    ```java
    // ...

    .filter(Objects::nonNull)

    // ...
    ```

+ In more ideal situations, when "nullability" is instead represented by `Optional`s being inhabited or empty, filter out empty `Optional`s while simultaneously unwrapping inhabited `Optional`s like so:

    ```java
    // ...

    .flatMap(Optional::stream)

    // ...
    ```

+ If you want to keep the `Optional`s intact in your stream but just filter out empty ones, use this instead:

    ```java
    // ...

    .filter(Optional::isPresent)

    // ...
    ```

+ If you are forced to deal with a stream or collection of nullable objects, you can nicely convert this to the use of `Optional`s like so:

    ```java
    // ...

    .map(Optional::ofNullable)

    // ...
    ```

+ Do **not** use any other implementation of the `List` interface (e.g. `LinkedList`) other than `ArrayList`, unless you know **exactly what you are doing**.
+ Prefer to use implementations of the `Map` interface (e.g. `HashMap`, `LinkedHashMap`) over types of the form `List<Pair<T, U>>`, unless you need to be able to index the mappings directly, need it for compatibility with other already existing methods, or you know **exactly** why you are using a `List` instead.
+ Prefer to use implementations of the `Set` interface (e.g. `TreeSet`, `HashSet`) over implementations of `List` when order doesn't matter and there will not be any duplicates (or duplicates can/should be eliminated). Again, if you **know** that the benefits of `Set` implementations are not needed, then a `ArrayList` works great (even better).
+ Eliminate (or at the very least, comment out) unused `import` statements.
+ When declaring local variables, choose to use `var` as the type whenever possible, unless it seriously hurts readability:
    - Bad:

        ```java
        for (final MapleCharacter chr : characterList) {
            // Do shit
        }

        final MapleMapObject mmo = currMap.getMapObjs().stream().findFirst(predicate);
        ```

    - Good:

        ```java
        for (final var chr : characterList) {
            // Do shit
        }

        final var mmo = currMap.getMapObjs().stream().findFirst(predicate);
        ```

+ In general, use interface types whenever you *do* specify the type of a local variable, field, or method parameter:
    - Bad:

        ```java
        private final ArrayList<String> guestList = new ArrayList<>(5);
        ```

    - Good:

        ```java
        private final List<String> guestList = new ArrayList<>(5);
        ```

+ Fields should be `final` by default. Obvious exceptions to this rule include fields that are Java primitives (`int`, `char`, `boolean`, etc.) and their wrapper classes (`Integer`, `Character`, `Boolean`, etc.), although even they should be `final` as well if they are constant:
    - Bad:

        ```java
        private ThisClass instance = new ThisClass();
        private Set<Integer> monsterLevels = new TreeSet<>();
        ```

    + Good:

        ```java
        private final ThisClass instance = new ThisClass();
        private final Set<Integer> monsterLevels = new TreeSet<>();
        ```

+ Fields should be `private` by default, or `protected` when appropriate.
+ Don't use package-private access level unless you know exactly what you are doing. Instead, explicitly specify the access level of a method or field to be `private`, `protected`, or `public`, as appropriate.
+ Don't use `.parallelStream()` unless you know *exactly* why you are doing it and *how it works*. Prefer `.stream()` instead.
+ Do not ignore caught exceptions unless there is an explicit and **controlled** reason for doing so.
+ When `catch`ing exceptions, catch the most specific exceptions possible; avoid `catch`ing `Throwable` or `Exception` unless absolutely appropriate.
+ Prefer to return an empty object (e.g. an empty `ArrayList`) over returning `null` when appropriate.
+ Instead of handling or returning possibly-`null` ("nullable") values, **use `Optional`**. Prefer not to deal directly with `null` values at all, especially in the context of returning them from a method. Good practice is to return `Optional.empty()` when a method has no "good" return value or it fails in some way to produce results, instead of returning the usual `null`:
    - Bad:

        ```java
        /**
         * <ul>
         * <li>pure?: false</li>
         * <li>nullable?: true</li>
         * </ul>
         */
        public ResultObj aMethod(final ParamType foo) {
            try {
                return tryToDoThing(foo);
            } catch (SomeException se) {
                return null;
            }
        }
        ```

    - Good:

        ```java
        /**
         * <ul>
         * <li>pure?: false</li>
         * <li>nullable?: false</li>
         * </ul>
         */
        public Optional<ResultObj> aMethod(final ParamType foo) {
            try {
                // Assume here that `tryToDoThing` is not nullable.
                return Optional.of(tryToDoThing(foo));
            } catch (SomeException se) {
                return Optional.empty();
            }
        }
        ```

+ **Always** use type-safe versions of things when they are available, e.g.:
    - Bad:

        ```java
        public Set<SpecificType> dummyMethod() {
            return Collections.EMPTY_SET;
        }
        ```

    - Good:

        ```java
        public Set<SpecificType> dummyMethod() {
            return Collections.emptySet();
        }
        ```

+ When implementing a "getter" method that returns some kind of `Collection`, consider the implementation carefully and **include a javadoc comment for the method** explaining exactly what it returns.
    - (In general do not do the following kind of implementation:) If you want callers of the method to be able to directly manipulate the `Collection` that they are "getting," i.e. you want them to have a reference to the `Collection` itself, then the method can work like a normal getter but should indicate that it hands off a direct reference (again, don't do this as it breaks encapsulation, very often produces bugs, and is often not what you really want anyways).
    - If you want callers to have a *view* of the `Collection`'s contents, real-time (even as they are possibly modified by other threads) but don't want them to be able to modify it (keeping encapsulation and reducing bugs), use the wrapper methods that come `static` with the `Collections` class:

        ```java
        public List<SpawnPoint> getOwnSpawns() {
            return Collections.unmodifiableList(ownSpawns);
        }
        ```

    - If you want callers to get a copy of the `Collection` *in the exact state it was in when they called the getter method* in order to keep encapsulation and **avoid possible concurrency errors(!)**, just use a constructor method (and make sure the javadoc lets callers know they are *not* getting a view of the collection). This method, while it is the safest, **is the most expensive** and thus should be used sparingly (usually the method listed above is better). Callers should be made *well-aware* that they are making an entirely new collection by copying every single element any time that they call such a method:

        ```java
        public List<SpawnPoint> getOwnSpawns() {
            return new ArrayList<>(ownSpawns);
        }
        ```

+ When performing arithmetic that mixes integral types (`int`, `long`, etc.) with floating-point types (`float`, `double`), prefer to cast integral types to floating-point types before doing the operations, and then cast back to an integral type if needed:
    - Bad:

        ```java
        final int fooInt = (int) ((barInt - bazInt) * quuxDouble);
        ```

    - Good:

        ```java
        final int fooInt = (int) ((double) (barInt - bazInt) * quuxDouble);
        ```

+ Prefer to use `static` methods (and `static` classes) over instanced/singleton classes.
+ Do **not** make unchecked assignments with parametrized types, i.e. always specify parameter type:
    - Bad:

        ```java
        final List<String> someStrings = new ArrayList();
        ```

    + Good:

        ```java
        final var someStrings = new ArrayList<String>();
        ```

+ When creating a new `Collection` object, it is generally a good idea to specify the parameters to the constructor unless the default parameters are the most appropriate or it cannot be known what parameters make sense until runtime:
    - (Probably) bad:

        ```java
        private final Map<Integer, MapleMapObject> mapObjects = new ConcurrentHashMap<>();
        ```

    - Good:

        ```java
        private final Map<Integer, MapleMapObject> mapObjects = new ConcurrentHashMap<>(10, 0.7f, 2);
        ```

+ **Do not use inheritance/extension of objects**. Instead, if some kind of "hierarchy" is needed or "supertype" is needed for some set of types, **use interfaces ONLY**. Just create an interface class (such a class only defines the signatures of functions, and *possibly* has implementations for `static`/`default` methods and `final static` constants) and then have "subobjects" which are concrete implementations that `implement` the interface.
+ When it's necessary to make explicit type coercions between non-primitive types (hopefully never), try to do so as early on as possible instead of making the coercion immediately before doing something with the new type. This generally should entail encapsulating the coercion and making some class return the value of the already-coerced type from one of its methods.
    - Bad:

        ```java
        // Room.java

        public List<Furniture> getFurniture() {
            return new ArrayList<>(furniture);
        }

        // Foo.java

        private void bar(final Room room) {
            room.getFurniture()
                .stream()
                .filter(furniture -> furniture instanceof Chair)
                .map(furniture -> (Chair) furniture)
                .forEach(this::doSomethingWithChairs);
        }
        ```

    - Good:

        ```java
        // Room.java

        public List<Furniture> getFurniture() {
            return new ArrayList<>(furniture);
        }

        public List<Chair> getChairs() {
            /*
             * Could also have a field in each class that implements Furniture
             * that indicates type using an enum value, but `instanceof` directly
             * checks for what we want.
             */
            return furniture.stream()
                            .filter(furniture -> furniture instanceof Chair)
                            .map(furniture -> (Chair) furniture)
                            .collect(Collectors.toList());
        }

        // Foo.java

        private void bar(final Room room) {
            room.getChairs()
                .forEach(this::doSomethingWithChairs);
        }
        ```

+ Whenever possible, make all fields, local variables, and method parameters that are declared outside of and used inside of an anonymous function explicitly `final`. (Fields should already be `final` anyways.)
+ Use the strongest access specifier that is appropriate when declaring methods, classes, and fields. Viz., prefer `private` over `protected` over `public`.
+ Methods that are to be called inside of scripts **must** be `public`, otherwise shit just won't work.
+ Do **not** use reflection, with the slight exception of `instanceof`.
+ When doing `String` concatenation many times into a single `String` (i.e. inside of a loop, `.forEach()`, or `.reduce()`), use `StringBuilder`, and make sure not to do any concatenation inside of `StringBuilder` method calls. Call `.append()` multiple times instead.
+ When dealing with `Collection` instances that are quite volatile and prone to concurrency errors, prefer to use synchronized variants or wrappers to the `Collection`, and use `Iterator`s instead of `Stream`s or `for` loops, like so:

    ```java
    private final Map<String> visibleNames = Collections.synchronizedSet(
                                                 new LinkedHashSet<>(35, 0.7f)
                                             );

    // Here we have two getter methods, for different purposes:

    /**
     * Gets an unmodifiable but direct and up-to-date view
     * of the currently visible names.
     *
     * @return Unmodifiable set of visible names.
     *
     * @pure: false
     * @nullable: false
     */
    public Set<String> getVisibleNames() {
        /*
         * Here we use a `synchronized` block because it's more
         * general-purpose, but in the case that the volatile field
         * in question is the only one that needs this treatment
         * and you are sure to synchronize all methods with the
         * capability to modify the collection and you are sure
         * that this specific instance of this class is the
         * only object that can directly modify the collection, it's
         * better to instead synchronize the methods themselves.
         */
        synchronized (visibleNames) {
            return Collections.unmodifiableSet(visibleNames);
        }
    }

    /**
     * Gets a snapshot of the currently visible names
     * at the time that this method is called.
     *
     * @return Static set of names.
     *
     * @pure: false
     * @nullable: false
     */
    public Set<String> readVisibleNames() {
        /*
         * Notice that this function is called "readVisibleNames"
         * to distinguish it from "getVisibleNames."
         */
        synchronized (visibleNames) {
            return new HashSet<>(visibleNames);
        }
    }

    /**
     * Adds a name to the set of currently
     * visible names.
     *
     * @param name The name to be added
     *
     * @pure: false
     */
    public void addVisibleName(final String name) {
        // We do not use `Iterator` here, since `Iterator`s cannot add.
        synchronized (visibleNames) {
            visibleNames.add(name);
        }
    }

    /**
     * Removes a name from the set of currently
     * visible names.
     *
     * @param name The name to be removed
     *
     * @pure: false
     */
    public void removeVisibleName(final String name) {
        synchronized (visibleNames) {
            var nameIter = visibleNames.iterator();
            while (nameIter.hasNext()) {
                String nextName = nameIter.next();
                if (name == null ? nextName == null : name.equals(nextName)) {
                    nameIter.remove();
                    break;
                }
            }
        }
    }

    /**
     * Checks if a name is currently visible or not.
     *
     * @param name The name to check for
     *
     * @pure: false
     */
    public boolean isNameVisible(final String name) {
        synchronized (visibleNames) {
            var nameIter = visibleNames.iterator();
            while (nameIter.hasNext()) {
                final var nextName = nameIter.next();
                if (name == null ? nextName == null : name.equals(nextName)) {
                    return true;
                }
            }

            return false;
        }
    }
    ```

+ When using a `Comparator` (e.g. for sorting), use the `static` methods provided by the `Comparator` class instead of an explicitly coded `Comparator` whenever there is one available.
+ Do not explicitly box or unbox values unless absolutely necessary (it shouldn't be).
