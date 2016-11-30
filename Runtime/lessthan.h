#pragma once

#include "declarations.h"

void lt__i() {
	LOAD(int, rhs, integerValue)
	LOAD(int, lhs, integerValue)
	VALUE_OP(lhs < rhs)
}

void lt__r() {
	LOAD(float, rhs, realValue)
	LOAD(float, lhs, realValue)
	VALUE_OP(lhs < rhs)
}

void lt__v() {

}
