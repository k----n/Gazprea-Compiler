#pragma once

#include "declarations.h"

void rightArrowOperator() {
	_unwrap();
	Value* rhs = stack->pop();
	_unwrap();
	Value* lhs = stack->pop();
	if (!rhs->isStandardOut()) { printf("RHS value is not stdout\n"); exit(1); }
	ValueType* type = lhs->getType();

	switch (type->getType()) {
		case NullType:
		case IdentityType:
		case BooleanType:
		case IntegerType:
		case RealType:
		case CharacterType:
//		case IntervalType:
		case VectorType:
			stack->push(lhs);
			printValue();
			break;
//		case MatrixType:
		case TupleType:     printf("Cannot print TupleType\n");     exit(1);
		case IntervalType:  printf("Cannot print IntervalType\n");  exit(1);
		case StandardOut:	printf("Cannot print StandardOut\n");	exit(1);
		case StandardIn:	printf("Cannot print StandardIn\n");	exit(1);
		case StartVector:	printf("Cannot print VectorStart\n");	exit(1);
		case Lvalue:		printf("Cannot print Lvalue\n");		exit(1);
	}
	type->release();
	lhs->release();
	rhs->release();
}

void leftArrowOperator() {
	_unwrap();
	Value* rhs = stack->pop();
	Value* lhs = stack->pop();
	if (!rhs->isStandardIn())	{ printf("RHS value is not stdin\n"); exit(1); }
	if (!lhs->isLvalue())		{ printf("LHS not an Lvalue\n"); exit(1); }
	char boolValue;
	int intValue;
	float realValue;
	char characterValue;
	int readErrorCode;
	Value* lvalue = lhs->lvalue();
	ValueType* type = lvalue->getType();
	switch (type->getType()) {
		case NullType:		printf("Cannot input NullType\n");		exit(1);
		case IdentityType:	printf("Cannot input IdentityType\n");	exit(1);
		case BooleanType:
			readErrorCode = scanf("%c", &boolValue);
			if (boolValue == 'T') {
				(*(Value**)lhs->lvalue_ptr())->release();
				*lhs->lvalue_ptr() = new Value(true);
			} else if (boolValue == 'F') {
				(*(Value**)lhs->lvalue_ptr())->release();
				*lhs->lvalue_ptr() = new Value(false);
			} else {
				// TODO: Handle errors
			}
			break;
		case IntegerType:
			readErrorCode = scanf("%d", &intValue);
			(*(Value**)lhs->lvalue_ptr())->release();
			*lhs->lvalue_ptr() = new Value(intValue);
			break;
		case RealType:
		    // TODO account for underscores in floats
			readErrorCode = scanf("%f", &realValue);
			(*(Value**)lhs->lvalue_ptr())->release();
			*lhs->lvalue_ptr() = new Value(realValue);
			break;
		case CharacterType:
			readErrorCode = scanf("%c", &characterValue);
			(*(Value**)lhs->lvalue_ptr())->release();
			*lhs->lvalue_ptr() = new Value(characterValue);
			break;
		case TupleType:     printf("Cannot input TupleType\n");		exit(1);
		case StandardOut:	printf("Cannot input StandardOut\n");	exit(1);
		case StandardIn:	printf("Cannot input StandardIn\n");	exit(1);
		case Lvalue:		printf("Cannot input Lvalue\n");		exit(1);
		case IntervalType:  printf("Cannot input IntervalType\n");  exit(1);
		case VectorType:    printf("Cannot input VectorType\n");    exit(1);
		case StartVector:	printf("Cannot input StartVector\n");	exit(1);
	}
	lvalue->release();
	type->release();
	switch (readErrorCode) {
		case 1: stdInputError = 0;
		case 0: stdInputError = 1;
		case EOF: stdInputError = 2;
	}
	lhs->release();
	rhs->release();
}
