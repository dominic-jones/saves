package org.dv.saves.extensions

import java.nio.file.Files
import java.nio.file.Path

fun Path.exists() = Files.exists(this)
fun Path.isDirectory() = Files.isDirectory(this)
