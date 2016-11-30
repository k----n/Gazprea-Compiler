#pragma once

#include "declarations.h"

void geq_i() {
	LOAD(int, rhs, integerValue)
	LOAD(int, lhs, integerValue)
	VALUE_OP(lhs >= rhs)
}

void geq_r() {
	LOAD(float, rhs, realValue)
	LOAD(float, lhs, realValue)
	VALUE_OP(lhs >= rhs)
}

void geq_v() {

}