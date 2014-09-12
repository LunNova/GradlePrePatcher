package me.nallar.gradleprepatcher.annotation;

/**
 * Makes an inner class (name must be specified) of the class which is being extended public
 */
public @interface ExposeInnerClass {
	String value();
}
