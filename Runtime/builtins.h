#pragma once

int stdInputError = 0;

void std_output()	{ pushEmptyValue(StandardOut);	}
void std_input()	{ pushEmptyValue(StandardIn);	}

void stream_state() {
	_unwrap();
	Value* stream = stack->pop();
	
	Value* value;
	switch (stream->getType()->getType()) {
		case NullType:
		case IdentityType:
		case BooleanType:
		case IntegerType:
		case RealType:
		case CharacterType:
		case TupleType:
		case IntervalType:
		case VectorType:
		case StandardOut:
		case Lvalue:
		case StartVector:
			printf("Argument was not a stream");
			exit(1);
		case StandardIn:
			value = new Value(stdInputError);
	}
	stack->push(value);
	value->release();
	stream->release();
}
