package com.avairebot.utilities;

import javax.annotation.Nullable;
import java.awt.*;
import java.lang.reflect.Field;

public class ColorUtil {

    /**
     * Creates an sRGBA color with the specified red, green,
     * blue, and alpha values with in the range (0 - 255).
     * The alpha should be in the rage of 0 and 100.
     *
     * @param red   The red component.
     * @param green The green component.
     * @param blue  The blue component.
     * @param alpha The alpha component.
     * @return The color with the given values.
     */
    public static Color makeColor(float red, float green, float blue, float alpha) {
        return new Color(red / 255F, green / 255F, blue / 255F, alpha / 100F);
    }

    /**
     * Creates an sRGB color with the specified red, green,
     * and blue values with in the range (0 - 255).
     *
     * @param red   The red component.
     * @param green The green component
     * @param blue  The blue component
     * @return The color with the given values.
     */
    public static Color makeColor(float red, float green, float blue) {
        return new Color(red / 255F, green / 255F, blue / 255F, 1F);
    }

    /**
     * Converts a given string into a color.
     *
     * @param value The string, either a name or a hex-string.
     * @return the color or null if an error occurs.
     */
    @Nullable
    public static Color getColorFromString(String value) {
        if (value == null) {
            return null;
        }

        try {
            // get color by hex or octal value
            return Color.decode(value);
        } catch (NumberFormatException nfe) {
            try {
                // if we can't decode lets try to get it by name
                // try to get a color by name using reflection
                final Field f = Color.class.getField(value);

                return (Color) f.get(null);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
