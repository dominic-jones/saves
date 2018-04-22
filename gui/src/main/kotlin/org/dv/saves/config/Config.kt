package org.dv.saves.config

import org.dv.saves.Saves
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackageClasses = [Saves::class])
open class Config