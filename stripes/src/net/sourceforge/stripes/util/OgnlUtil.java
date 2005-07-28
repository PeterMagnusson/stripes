package net.sourceforge.stripes.util;

import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlException;
import ognl.OgnlRuntime;

import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Method;
import java.beans.IntrospectionException;

/**
 * Utility class used to configure and use Ognl in a consistent manner across the pieces of
 * Stripes that use it.  Ensures that the Stripes specific OgnlCustomNullHandler is registered with
 * the OgnlRuntime before it is used, and that the appropriate OgnlContexts are used.  Also provides
 * facilities for caching the Ognl expression objects that are created from parsing property names -
 * this provides a significant performance boost.
 *
 * @author Tim Fennell
 */
public class OgnlUtil {
    /** Store for pre-parsed expressions. */
    private static Map<String,Object> expressions = new HashMap<String,Object>();

    /** Static initializer block that inserts a custom NullHandler into the OgnlRuntime. */
    static {
        OgnlRuntime.setNullHandler(Object.class, new OgnlCustomNullHandler());
    }

    /** Private default constructor to prevent anyone from instantiating the class. */
    private OgnlUtil() {
        // Do Nothing
    }

    /**
     * Fetches the value of a named property (nested, indexed etc.) using Ognl.
     *
     * @param property an expression representing the property desired
     * @param root the object from which to fetch the property
     * @return Object the value of the property, or null if the property is null
     * @throws OgnlException thrown when a problem occurs such as one or more properties not
     *         existing or being inaccessible
     */
    public static Object getValue(String property, Object root) throws OgnlException {
        return Ognl.getValue(getExpression(property), createContext(), root);
    }

    /**
     * Sets the value of a named property (nested, indexed etc.) using Ognl. If any of the values
     * along the way are not set then the values are created and set into the object graph.
     *
     * @param property an expression representing the property to set
     * @param root the object on which to set the property
     * @param value the value of the property to be set
     * @throws OgnlException thrown when a problem occurs such as one or more properties not
     *         existing or being inaccessible
     */
    public static void setValue(String property, Object root, Object value) throws OgnlException {
        Ognl.setValue(getExpression(property), createContext(), root, value);
    }

    /**
     * Retrieves the class type of the specified property without instantiating the property
     * itself. All earlier properties in a chain will, however, be instantiated.
     *
     * @param property an expression for the property to be inspected
     * @param root the object on which the property lives
     * @return the Class type of the property
     * @throws OgnlException thrown when a problem occurs such as one or more properties not
     *         existing or being inaccessible
     */
    public static Class getPropertyClass(String property, Object root) throws OgnlException,
                                                                              IntrospectionException {
        int propertyIndex = property.lastIndexOf(".");
        if (propertyIndex > 0) {
            String parentProperty = property.substring(0,propertyIndex);
            String childProperty = property.substring(propertyIndex+1);

            // foo.bar.baz.splat => parent: "foo.bar.baz" and child: "id"

            root = getValue(parentProperty,root);
            property = childProperty;
        }

        Method method =
                OgnlRuntime.getGetMethod(createContext(), root.getClass(), property);

        return method.getReturnType();
    }

    /**
     * Supplies the Ognl expression object for a given property name string.  The property name
     * can be any combination of the types that Ognl can handle (simple, indexed, nested etc.).
     * First checks to see if a cached expression object exists, and if so supplied that. If no
     * cached expression exists, one is manufactured, placed into the cache and then returned.
     *
     * @param property the property name string being used to access a property
     * @return an Ognl expression object representing the property name string
     * @throws OgnlException if the property name is not a well formed Ognl property name string
     */
    private static synchronized Object getExpression(String property) throws OgnlException {
        Object ognlExpression = OgnlUtil.expressions.get(property);
        if (ognlExpression == null) {
            ognlExpression = Ognl.parseExpression(property);
            OgnlUtil.expressions.put(property, ognlExpression);
        }

        return ognlExpression;
    }

    /**
     * Manufactures an OgnlContext configured as needed for Stripes to operate correctly.
     *
     * @return a pre-configured OgnlContext
     */
    private static OgnlContext createContext() {
        OgnlContext context = new OgnlContext();
        context.setTraceEvaluations(true);
        return context;
    }
}
