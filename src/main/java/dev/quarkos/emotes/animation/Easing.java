package dev.quarkos.emotes.animation;

import java.util.Locale;
public final class Easing {
    private Easing() {}
    public static double apply(String name, double t) {
        t = Math.max(0, Math.min(1, t));
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "ease_in", "ease_in_quad" -> t*t;
            case "ease_out", "ease_out_quad" -> 1-(1-t)*(1-t);
            case "ease_in_out", "ease_in_out_quad" -> t<.5 ? 2*t*t : 1-Math.pow(-2*t+2,2)/2;
            case "ease_in_cubic" -> t*t*t;
            case "ease_out_cubic" -> 1-Math.pow(1-t,3);
            case "ease_in_out_cubic" -> t<.5 ? 4*t*t*t : 1-Math.pow(-2*t+2,3)/2;
            case "ease_in_sine" -> 1-Math.cos(t*Math.PI/2);
            case "ease_out_sine" -> Math.sin(t*Math.PI/2);
            case "ease_in_out_sine" -> -(Math.cos(Math.PI*t)-1)/2;
            case "ease_in_back" -> 2.70158*t*t*t-1.70158*t*t;
            case "ease_out_back" -> 1+2.70158*Math.pow(t-1,3)+1.70158*Math.pow(t-1,2);
            case "ease_in_out_back" -> t<.5 ? Math.pow(2*t,2)*((2.5949095+1)*2*t-2.5949095)/2
                    : (Math.pow(2*t-2,2)*((2.5949095+1)*(t*2-2)+2.5949095)+2)/2;
            default -> t;
        };
    }
}
