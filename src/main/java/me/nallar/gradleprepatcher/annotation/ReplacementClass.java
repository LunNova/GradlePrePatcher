package me.nallar.gradleprepatcher.annotation;

/**
 * This interface marks a class as being used to replace the class it extends.
 * Used in cases where extending the parent class is required so that methods/fields will be remapped
 */
public @interface ReplacementClass {
}
