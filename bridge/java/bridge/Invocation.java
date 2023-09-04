package bridge;

/**
 * A class that can be used to access field &amp; method bridges (and other things too)
 * @implNote Imaginary objects defined within and returned by this class may not be stored. In other words, you are required to use them in the same statement they were created in for your code to compile correctly.
 */
@SuppressWarnings("InnerClassMayBeStatic")
public final class Invocation {

    private static native <T> T $();

    /**
     * Creates fork(s) of your class with code tailored to the specified Java language level(s)
     *
     * @implNote This constant may only appear in integer comparisons as the first operand. Using it anywhere else will not compile correctly.
     */
    public static final int LANGUAGE_LEVEL = $();

    /**
     * Starts a direct reference to something static
     *
     * @param type Type constant &ndash; no dynamic values are permitted here.
     */
    public Invocation(Class<?> type) {}

    /**
     * Starts a direct reference to something static
     *
     * @param type Type name constant &ndash; no dynamic values or string manipulation is permitted here.
     */
    public Invocation(String type) {}

    /**
     * Starts a direct reference to something that belongs to an instance of an object
     *
     * @implNote Calling this overload will cause the object type to be determined automatically.
     * @param instance Object instance
     */
    public Invocation(@Polymorphic Object instance) {}

    /**
     * Starts a direct reference to something that belongs to an instance of an object
     *
     * @param type Type constant &ndash; no dynamic values are permitted here.
     * @param instance Object instance
     */
    public Invocation(Class<?> type, Object instance) {}

    /**
     * Starts a direct reference to something that belongs to an instance of an object
     *
     * @param type Type name constant &ndash; no dynamic values or string manipulation is permitted here.
     * @param instance Object instance
     */
    public Invocation(String type, Object instance) {}

    /**
     * Creates a direct reference to a class
     *
     * @return An actual class reference.
     */
    public Class<?> ofClassLiteral() { return $(); }

    /**
     * Creates a direct type-compatibility check
     *
     * @implNote This operation requires instance access.
     * @return The actual boolean response.
     */
    public boolean ofInstanceOf() { return $(); }

    /**
     * Creates a direct reference to a constructor
     *
     * @implNote This operation requires static access.
     * @return An imaginary reference for you to execute your request with.
     */
    public Executor ofConstructor() { return $(); }

    /**
     * Creates a direct reference to a method
     *
     * @implNote Calling this overload will cause the method return type to be determined automatically.
     * @param name Method name constant &ndash; no dynamic values or string manipulation is permitted here.
     * @return An imaginary reference for you to execute your request with.
     */
    public Executor ofMethod(String name) { return $(); }

    /**
     * Creates a direct reference to a method
     *
     * @param type Method return type constant &ndash; no dynamic values are permitted here.
     * @param name Method name constant &ndash; no dynamic values or string manipulation is permitted here.
     * @return An imaginary reference for you to execute your request with.
     */
    public Executor ofMethod(Class<?> type, String name) { return $(); }

    /**
     * Creates a direct reference to a method
     *
     * @param type Method return type name constant &ndash; no dynamic values or string manipulation is permitted here.
     * @param name Method name constant &ndash; no dynamic values or string manipulation is permitted here.
     * @return An imaginary reference for you to execute your request with.
     */
    public Executor ofMethod(String type, String name) { return $(); }

    /**
     * A class that is used to execute method invocations
     */
    public final class Executor {
        private Executor() {}

        /**
         * Adds an argument to the stack
         *
         * @implNote Calling this overload will cause the parameter type to be determined automatically.
         * @param argument Argument value
         * @return An imaginary reference for you to continue executing your request with.
         */
        public Executor with(@Polymorphic Object argument) { return $(); }
        /**@hidden */public Executor with(double argument) { return $(); }
        /**@hidden */public Executor with(long argument) { return $(); }
        /**@hidden */public Executor with(float argument) { return $(); }
        /**@hidden */public Executor with(int argument) { return $(); }
        /**@hidden */public Executor with(short argument) { return $(); }
        /**@hidden */public Executor with(byte argument) { return $(); }
        /**@hidden */public Executor with(char argument) { return $(); }
        /**@hidden */public Executor with(boolean argument) { return $(); }

        /**
         * Adds an argument to the stack
         *
         * @param parameter Parameter type constant &ndash; no dynamic values are permitted here.
         * @param argument Argument value
         * @return An imaginary reference for you to continue executing your request with.
         */
        public Executor with(Class<?> parameter, Object argument) { return $(); }

        /**
         * Adds an argument to the stack
         *
         * @param parameter Parameter type name constant &ndash; no dynamic values or string manipulation is permitted here.
         * @param argument Argument value
         * @return An imaginary reference for you to continue executing your request with.
         */
        public Executor with(String parameter, Object argument) { return $(); }

        /**
         * Brings an exception into scope
         *
         * @param <E> Exception type
         * @return An imaginary reference for you to continue executing your request with.
         */
        public <E extends Throwable> Executor check() throws E { return $(); }

        /**
         * Brings an exception into scope
         *
         * @param exception Exception type constant &ndash; no dynamic values are permitted here.
         * @return An imaginary reference for you to continue executing your request with.
         */
        public <E extends Throwable> Executor check(Class<E> exception) throws E { return $(); };

        /**
         * Executes the method
         *
         * @return The actual return value of the method
         */
        public <@Polymorphic T> T invoke() { return $(); }
    }

    /**
     * Creates a direct reference to a field
     *
     * @implNote Calling this overload will cause the field type to be determined automatically.
     * @param name Field name constant &ndash; no dynamic values or string manipulation is permitted here.
     * @return An imaginary reference for you to execute your request with.
     */
    public Accessor ofField(String name) { return $(); }

    /**
     * Creates a direct reference to a field
     *
     * @param type Field type constant &ndash; no dynamic values are permitted here.
     * @param name Field name constant &ndash; no dynamic values or string manipulation is permitted here.
     * @return An imaginary reference for you to execute your request with.
     */
    public Accessor ofField(Class<?> type, String name) { return $(); }

    /**
     * Creates a direct reference to a field
     *
     * @param type Field type name constant &ndash; no dynamic values or string manipulation is permitted here.
     * @param name Field name constant &ndash; no dynamic values or string manipulation is permitted here.
     * @return An imaginary reference for you to execute your request with.
     */
    public Accessor ofField(String type, String name) { return $(); }

    /**
     * A class that is used to execute field invocations
     */
    public final class Accessor {
        private Accessor() {}

        /**
         * Gets the field value
         *
         * @return The current value the field holds
         */
        public <@Polymorphic T> T get() { return $(); }


        /**
         * Gets, then sets, the field value
         *
         * @param value The new value the field will hold
         * @return The previous value the field held
         */
        public <@Polymorphic T> T getAndSet(Object value) { return $(); }
        /**@hidden */public <@Polymorphic T> T getAndSet(double value) { return $(); }
        /**@hidden */public <@Polymorphic T> T getAndSet(long value) { return $(); }
        /**@hidden */public <@Polymorphic T> T getAndSet(float value) { return $(); }
        /**@hidden */public <@Polymorphic T> T getAndSet(int value) { return $(); }
        /**@hidden */public <@Polymorphic T> T getAndSet(short value) { return $(); }
        /**@hidden */public <@Polymorphic T> T getAndSet(byte value) { return $(); }
        /**@hidden */public <@Polymorphic T> T getAndSet(char value) { return $(); }
        /**@hidden */public <@Polymorphic T> T getAndSet(boolean value) { return $(); }


        /**
         * Sets the field value
         *
         * @param value The new value the field will hold
         * @return That same value (without any conversions)
         */
        public <T> T set(@Polymorphic T value) { return $(); }
        /**@hidden */public double set(double value) { return $(); }
        /**@hidden */public long set(long value) { return $(); }
        /**@hidden */public float set(float value) { return $(); }
        /**@hidden */public int set(int value) { return $(); }
        /**@hidden */public short set(short value) { return $(); }
        /**@hidden */public byte set(byte value) { return $(); }
        /**@hidden */public char set(char value) { return $(); }
        /**@hidden */public boolean set(boolean value) { return $(); }


        /**
         * Sets, then gets, the field value
         *
         * @param value The new value the field will hold
         * @return That same value (with all required conversions applied)
         */
        public <@Polymorphic T> T setAndGet(Object value) { return $(); }
        /**@hidden */public <@Polymorphic T> T setAndGet(double value) { return $(); }
        /**@hidden */public <@Polymorphic T> T setAndGet(long value) { return $(); }
        /**@hidden */public <@Polymorphic T> T setAndGet(float value) { return $(); }
        /**@hidden */public <@Polymorphic T> T setAndGet(int value) { return $(); }
        /**@hidden */public <@Polymorphic T> T setAndGet(short value) { return $(); }
        /**@hidden */public <@Polymorphic T> T setAndGet(byte value) { return $(); }
        /**@hidden */public <@Polymorphic T> T setAndGet(char value) { return $(); }
        /**@hidden */public <@Polymorphic T> T setAndGet(boolean value) { return $(); }
    }
}
