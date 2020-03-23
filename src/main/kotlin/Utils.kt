fun String.loadFromResources(): String {
    return ClassLoader.getSystemResource(this).readText()
}