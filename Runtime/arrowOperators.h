#pragma once

void rightArrowOperator() {
	_unwrap();
	Value* rhs = stack->pop();
	_unwrap();
	Value* lhs = stack->pop();
	if (!rhs->isStandardOut()) { printf("RHS value is not stdout\n"); exit(1); }
	switch (lhs->getType()->getType()) {
		case NullType:		printf("Cannot print NullType\n");		exit(1);
		case IdentityType:	printf("Cannot print IdentityType\n");	exit(1);
//		case BooleanType:
		case IntegerType:
//		case RealType:
//		case CharacterType:
//		case IntervalType:
//		case VectorType:
			stack->push(lhs);
			printValue();
			break;
//		case MatrixType:
//		case TupleType:
		case StandardOut:	printf("Cannot print StandardOut\n");	exit(1);
		case StandardIn:	printf("Cannot print StandardIn\n");	exit(1);
//		case VectorStart:	printf("Cannot print VectorStart\n");	exit(1);
		case Lvalue:		printf("Cannot print Lvalue\n");		exit(1);
	}
	lhs->release();
	rhs->release();
}

void leftArrowOperator() {
	_unwrap();
	Value* rhs = stack->pop();
	Value* lhs = stack->pop();
	if (!rhs->isStandardIn())	{ printf("RHS value is not stdin\n"); exit(1); }
	if (!lhs->isLvalue())		{ printf("LHS not an Lvalue\n"); exit(1); }
	int intValue;
	switch (lhs->lvalue()->getType()->getType()) {
		case NullType:		printf("Cannot input NullType\n");		exit(1);
		case IdentityType:	printf("Cannot input IdentityType\n");	exit(1);
		case IntegerType:
			scanf("%d", &intValue);
			(*(Value**)lhs->lvalue_ptr())->release();
			*lhs->lvalue_ptr() = new Value(intValue);
			break;
		case StandardOut:	printf("Cannot input StandardOut\n");	exit(1);
		case StandardIn:	printf("Cannot input StandardIn\n");	exit(1);
		case Lvalue:		printf("Cannot input Lvalue\n");		exit(1);
	}
	lhs->release();
	rhs->release();
}
