package il.arik.nadlantracker

object Fixtures {
    fun read(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("fixtures/$name")) {
            "fixture not found: $name"
        }.readText()
}
