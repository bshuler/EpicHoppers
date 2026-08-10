package com.songoda.epichoppers.api;


import org.bukkit.ChatColor;

/**
 * The access point of the EpicHoppersAPI, a class acting as a bridge between API
 * and plugin implementation. It is from here where developers should access the
 * important and core methods in the API. All static methods in this class will
 * call directly upon the implementation at hand (in most cases this will be the
 * EpicHoppers plugin itself), therefore a call to {@link #getImplementation()} is
 * not required and redundant in most situations. Method calls from this class are
 * preferred the majority of time, though an instance of {@link EpicHoppers} may
 * be passed if absolutely necessary.
 *
 * @see EpicHoppers
 * @since 3.0.0
 */
public class EpicHoppersAPI {

    private static EpicHoppers implementation;

    /**
     * Set the EpicHoppers implementation. Throws if an implementation is
     * already registered and has not been cleared via
     * {@link #clearImplementation()} first - presumably by the EpicHoppers
     * plugin's {@code onDisable()}, so that a plugin reload (e.g. a server
     * {@code /reload}, or a plugin manager like PlugMan re-enabling this
     * plugin without a JVM restart) can re-register a fresh instance instead
     * of permanently wedging the API after the very first enable.
     *
     * @param implementation the implementation to set
     */
    public static void setImplementation(EpicHoppers implementation) {
        if (EpicHoppersAPI.implementation != null) {
            throw new IllegalArgumentException("Cannot set API implementation twice");
        }

        EpicHoppersAPI.implementation = implementation;
    }

    /**
     * Clear the currently registered implementation, allowing
     * {@link #setImplementation(EpicHoppers)} to be called again. Called by
     * {@code EpicHoppersPlugin#onDisable()} so the plugin can be safely
     * disabled and re-enabled within the same JVM.
     */
    public static void clearImplementation() {
        EpicHoppersAPI.implementation = null;
    }

    /**
     * Get the EpicHoppers implementation. This method may be redundant in most
     * situations as all methods present in {@link EpicHoppers} will be mirrored
     * with static modifiers in the {@link EpicHoppersAPI} class
     *
     * @return the EpicHoppers implementation
     */
    public static EpicHoppers getImplementation() {
        return implementation;
    }
}
