#pragma once

enum BuiltinType {
	NullType,
	IdentityType,
//	BooleanType,
	IntegerType,
//	RealType,
//	CharacterType,
//	IntervalType,
//	VectorType,
//	MatrixType,
//	TupleType,
	
	StandardOut,
	StandardIn,
	
//	VectorStart,
	
	Lvalue,
};

class Type {
private:
	BuiltinType type;
//	Vector<Type>* subtypes;
	
public:
	Type(BuiltinType type);
	~Type();
	
	BuiltinType getType();
//	Vector<Type>* getSubtypes();
};
