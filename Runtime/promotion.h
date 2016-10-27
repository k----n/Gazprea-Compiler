#pragma once

void swapStackTopTwo() {
	Value* topValue = stack->pop();
	Value* secondValue = stack->pop();
	stack->push(topValue);
	stack->push(secondValue);
	topValue->release();
	secondValue->release();
}

void promoteTo_b() {
	Value* value = stack->pop();
	Value* newValue;
	int* intValue;
	char* charValue;
	switch (value->getType()->getType()) {
		case NullType: printf("Cannot promote Null\n"); exit(1);
		case IdentityType: printf("Cannot promote Identity\n"); exit(1);
		case BooleanType:
			newValue = value;
			newValue->retain();
			break;
		case IntegerType:
			intValue = value->integerValue();
			newValue = new Value(*intValue != 0);
			break;
		case RealType: printf("Cannot promote Real\n"); exit(1);
		case CharacterType:
			charValue = value->characterValue();
			newValue = new Value(*charValue != '\0');
			break;
		case StandardOut: printf("Cannot promote stdout\n"); exit(1);
		case StandardIn: printf("Cannot promote stdin\n"); exit(1);
		case Lvalue:
			newValue = value->lvalue();
			stack->push(newValue);
			newValue->release();
			return promoteTo_b();
	}
	stack->push(newValue);
	newValue->release();
	value->release();
}

void promoteTo_i() {
	Value* value = stack->pop();
	Value* newValue;
	bool* boolValue;
	float* realValue;
	char* charValue;
	switch (value->getType()->getType()) {
		case NullType: printf("Cannot promote Null\n"); exit(1);
		case IdentityType: printf("Cannot promote Identity\n"); exit(1);
		case BooleanType:
			boolValue = value->booleanValue();
			newValue = new Value(*boolValue ? 1 : 0);
			break;
		case IntegerType:
			newValue = value;
			newValue->retain();
			break;
		case RealType:
			realValue = value->realValue();
			newValue = new Value((int)*realValue);
			break;
		case CharacterType:
			charValue = value->characterValue();
			newValue = new Value((int)*charValue);
			break;
		case StandardOut: printf("Cannot promote stdout\n"); exit(1);
		case StandardIn: printf("Cannot promote stdin\n"); exit(1);
		case Lvalue:
			newValue = value->lvalue();
			stack->push(newValue);
			newValue->release();
			return promoteTo_i();
	}
	stack->push(newValue);
	newValue->release();
	value->release();
}

void promoteTo_r() {
	Value* value = stack->pop();
	Value* newValue;
	bool* boolValue;
	int* intValue;
	char* charValue;
	switch (value->getType()->getType()) {
		case NullType: printf("Cannot promote Null\n"); exit(1);
		case IdentityType: printf("Cannot promote Identity\n"); exit(1);
		case BooleanType:
			boolValue = value->booleanValue();
			newValue = new Value(*boolValue ? 1.0f : 0.0f);
			break;
		case IntegerType:
			intValue = value->integerValue();
			newValue = new Value((float)*intValue);
			break;
		case RealType:
			newValue = value;
			newValue->retain();
			break;
		case CharacterType:
			charValue = value->characterValue();
			newValue = new Value((float)*charValue);
			break;
		case StandardOut: printf("Cannot promote stdout\n"); exit(1);
		case StandardIn: printf("Cannot promote stdin\n"); exit(1);
		case Lvalue:
			newValue = value->lvalue();
			stack->push(newValue);
			newValue->release();
			return promoteTo_r();
	}
	stack->push(newValue);
	newValue->release();
	value->release();
}

void promoteTo_c() {
	Value* value = stack->pop();
	Value* newValue;
	bool* boolValue;
	int* intValue;
	switch (value->getType()->getType()) {
		case NullType: printf("Cannot promote Null\n"); exit(1);
		case IdentityType: printf("Cannot promote Identity\n"); exit(1);
		case BooleanType:
			boolValue = value->booleanValue();
			newValue = new Value((char)(*boolValue ? 1 : 0));
			break;
		case IntegerType:
			intValue = value->integerValue();
			newValue = new Value((char)(*intValue % 256));
			break;
		case RealType: printf("Cannot promote real\n"); exit(1);
		case CharacterType:
			newValue = value;
			newValue->retain();
			break;
		case StandardOut: printf("Cannot promote stdout\n"); exit(1);
		case StandardIn: printf("Cannot promote stdin\n"); exit(1);
		case Lvalue:
			newValue = value->lvalue();
			stack->push(newValue);
			newValue->release();
			return promoteTo_c();
	}
	stack->push(newValue);
	newValue->release();
	value->release();
}
