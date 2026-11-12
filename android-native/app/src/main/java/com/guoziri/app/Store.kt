fun updateSettings(patch: (Settings) -> Settings) {
        synchronized(lock) { settings = patch(settings); writeSettings() }
        ThemePrefs.theme.value = settings.theme
    }