package org.emdev.ui.actions;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
public @interface ActionMethod {

	/**
	 * id of primary action called this method.
	 */
	int[] ids();
}
