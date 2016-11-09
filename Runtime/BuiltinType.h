#pragma once

enum BuiltinType {
	NullType,
	IdentityType,
	
	BooleanType,
	IntegerType,
	RealType,
	CharacterType,

    TupleType, // a vector with many different types included
    IntervalType, // a vector with only two nodes (lower bound and upper bound)
    //VectorType, // a vector
    //MatrixType, // a vector of vectors

	StandardOut,
	StandardIn,

	Lvalue,
	
	StartVector,
};
