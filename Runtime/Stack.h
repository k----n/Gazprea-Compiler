#pragma once

template <class T>
class Stack {
private:
	StackNode* topNode;
	
public:
	Stack();
	~Stack();
	
	/// pushes a copy - free the original manually
	void push(T* element);
	
	/// free the value
	T* pop();
};
