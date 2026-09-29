package org.kingpixel.cobblemonpatches.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * Utility for parsing color codes (& and §), hex colors (&#RRGGBB),
 * and formatting into Minecraft {@link Component} components.
 */
public final class TextUtils {

  private static final Pattern FORMAT_PATTERN = Pattern.compile("(?i)[&§]#([0-9a-f]{6})|[&§]([0-9a-fk-or])");

  private TextUtils() {
  }

  /**
   * Converts a formatted string with color codes into a Minecraft {@link Component} component.
   *
   * @param input the string containing legacy or hex color codes
   * @return formatted {@link MutableComponent}
   */
  public static MutableComponent parse(String input) {
    if (input == null || input.isEmpty()) {
      return Component.empty();
    }

    MutableComponent root = Component.empty();
    Matcher matcher = FORMAT_PATTERN.matcher(input);
    int lastEnd = 0;
    Style currentStyle = Style.EMPTY;

    while (matcher.find()) {
      if (matcher.start() > lastEnd) {
        String textPart = input.substring(lastEnd, matcher.start());
        root.append(Component.literal(textPart).setStyle(currentStyle));
      }

      String hex = matcher.group(1);
      String legacy = matcher.group(2);

      if (hex != null) {
        int rgb = Integer.parseInt(hex, 16);
        currentStyle = Style.EMPTY.withColor(TextColor.fromRgb(rgb));
      } else if (legacy != null) {
        currentStyle = applyLegacyCode(currentStyle, legacy.charAt(0));
      }

      lastEnd = matcher.end();
    }

    if (lastEnd < input.length()) {
      String remaining = input.substring(lastEnd);
      root.append(Component.literal(remaining).setStyle(currentStyle));
    }

    return root;
  }

  private static Style applyLegacyCode(Style style, char code) {
    ChatFormatting formatting = ChatFormatting.getByCode(Character.toLowerCase(code));
    if (formatting == null || formatting == ChatFormatting.RESET) {
      return Style.EMPTY;
    }
    if (formatting.isColor()) {
      return Style.EMPTY.applyFormat(formatting);
    }
    return style.applyFormat(formatting);
  }
}
