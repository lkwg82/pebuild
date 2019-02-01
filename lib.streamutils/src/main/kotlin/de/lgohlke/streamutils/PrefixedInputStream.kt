package de.lgohlke.streamutils

import java.io.InputStream


data class PrefixedInputStream constructor(val stream: InputStream,
                                           val prefix: String)