package voiidstudios.wonderevents.core.log;

public enum WonderLogMessages {
    // ---- 0xx - Internal WonderEvents errors (not the dev's fault) ----
    CANT_READ_MANIFEST(
        "WM-001",
        "[ERROR %s] Unable to load %s.jar (%s)! An internal WonderEvents error occurred while reading its `wonder-manifest.yml` file. Please open an issue on GitHub and attach this log along with the error details:"
    ),
    INTERNAL_CLASSLOADER_ERROR(
        "WM-002",
        "[ERROR %s] Unable to create the class loader for the %s %s! An internal WonderEvents error occurred. Please open an issue on GitHub and attach this log along with the error details:"
    ),

    // ---- 1xx - Problems with the manifest (wonder-manifest.yml) or its declared requirements ----
    MISSING_MANIFEST(
        "WM-100",
        "[ERROR %s] Unable to load %s.jar (%s)! Is it a Spigot plugin? It's missing `wonder-manifest.yml` in the root directory of the .jar file. Contact the developer and attach this log."
    ),
    CANNOT_PARSE_MANIFEST(
        "WM-101",
        "[ERROR %s] Unable to load %s.jar (%s)! It appears to have a malformed `wonder-manifest.yml` file — is it valid YAML? Contact the developer and attach this log along with the error details:"
    ),
    MISSING_BOOTSTRAP(
        "WM-102",
        "[ERROR %s] Unable to load the %s %s! A bootstrap class needs to be declared on `wonder-manifest.yml` to load. Contact the developer and attach this log."
    ),
    BOOTSTRAP_NOT_ASSIGNABLE(
        "WM-103",
        "[ERROR %s] Unable to load the %s %s! The declared bootstrap class on `wonder-manifest.yml` does not implement `WEABootstrap`. Contact the developer and attach this log."
    ),
    API_VERSION_MISMATCH(
        "WM-104",
        "[ERROR %s] Unable to load the %s %s! It looks like it was built against a different/older WonderEvents API (%s: %s). Contact the developer and attach this log."
    ),

    // ---- 2xx - The bootstrap's own code threw during a lifecycle call ----
    INIT_ERROR(
        "WM-200",
        "[ERROR %s] Error initializing the %s %s! It threw an error in the `init()` function. Contact the developer and attach this log along with the error details:"
    ),
    ONLOAD_ERROR(
        "WM-201",
        "[ERROR %s] Error initializing the %s %s! It threw an error in the `onLoad()` function. Contact the developer and attach this log along with the error details:"
    ),
    ONENABLE_ERROR(
        "WM-202",
        "[ERROR %s] Error enabling the %s %s! It threw an error in the `onEnable()` function. Contact the developer and attach this log along with the error details:"
    ),
    ONDISABLE_ERROR(
        "WM-203",
        "[ERROR %s] Error disabling the %s %s! It threw an error in the `onDisable()` function. Contact the developer and attach this log along with the error details:"
    ),
    ONRELOAD_ERROR(
        "WM-204",
        "[ERROR %s] Error reloading the %s %s! It threw an error in the `onReload()` function. Contact the developer and attach this log along with the error details:"
    ),
    CLEANRUNTIME_ERROR(
        "WM-205",
        "[ERROR %s] Error cleaning the %s %s! Its cleanup runtime threw an error. Contact the developer and attach this log along with the error details:"
    ),
    BOOTSTRAP_INSTANTIATION_ERROR(
        "WM-206",
        "[ERROR %s] Unable to load the %s %s! The bootstrap class could not be instantiated. Contact the developer and attach this log along with the error details:"
    ),

    // ---- 3xx - End-user issues (not the dev's fault): version/platform/dependency mismatches ----
    MIN_CORE_VERSION(
        "WM-300",
        "[ERROR %s] Unable to load the %s %s! It requires WonderEvents %s or newer, but the current version is %s"
    ),
    MAX_CORE_VERSION(
        "WM-301",
        "[ERROR %s] Unable to load the %s %s! It requires WonderEvents %s or older, but the current version is %s"
    ),
    MIN_MINECRAFT_VERSION(
        "WM-302",
        "[ERROR %s] Unable to load the %s %s! It requires Minecraft %s or newer, but the server is running %s."
    ),
    MAX_MINECRAFT_VERSION(
        "WM-303",
        "[ERROR %s] Unable to load the %s %s! It requires Minecraft %s or older, but the server is running %s"
    ),
    MISSING_PLUGIN_DEPENDENCY(
        "WM-304",
        "[ERROR %s] Unable to load the %s %s! It requires %s (plugin), but the condition is not met."
    ),
    MISSING_PLATFORM_DEPENDENCY(
        "WM-305",
        "[ERROR %s] Unable to load the %s %s! It requires %s (platform), but the condition is not met."
    ),
    MISSING_DEPENDENCY(
        "WM-306",
        "[ERROR %s] Unable to load the %s %s! It requires the dependency %s, but it is not present."
    ),

    // ---- 4xx - Non-fatal warnings ----
    ALREADY_LOADED(
        "WM-400",
        "[WARNING %s] The %s (%s) %s is already loaded. Do you have a duplicate .jar file? Skipping the loading of %s.jar"
    );

    private final String code;
    private final String template;

    WonderLogMessages(String code, String template) {
        this.code = code;
        this.template = template;
    }

    public String format(Object... args) {
        Object[] fullArgs = new Object[args.length + 1];
        fullArgs[0] = code;
        System.arraycopy(args, 0, fullArgs, 1, args.length);
        return String.format(template, fullArgs);
    }
}