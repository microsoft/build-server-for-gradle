package com.example.project

import spock.lang.Specification

class SpockTest extends Specification {
    def "zero is zero"() {
        expect:
        0 == 0
    }
}