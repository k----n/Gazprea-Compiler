#pragma once

#include "notAllowedImports.h"
#include "declarations.h"

// TODO fix not allowed imports

void exp_i() {
	LOAD(int, rhs, integerValue)
	LOAD(int, lhs, integerValue)
	VALUE_OP((int)pow((float)lhs, (float)rhs))
}

void exp_r() {
	LOAD(float, rhs, realValue)
	LOAD(float, lhs, realValue)
	VALUE_OP(powf(lhs, rhs))
}

void exp_v() {
    // TODO
    printf("EXPONETIATION OF VECTORS NOT IMPLEMENTED YET\n");
    exit(1);
}