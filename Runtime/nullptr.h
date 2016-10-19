#pragma once

#ifdef LLVM_NULLPTR
const class nullptr_t {
public:
	template<class T>
	inline operator T*() const { return 0; }
	
	template<class C, class T>
	inline operator T C::*() const { return 0; }
	
private:
	void operator&() const;
} nullptr = {};
#endif
