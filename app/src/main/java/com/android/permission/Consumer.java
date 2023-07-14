package com.android.permission;

public interface Consumer<T> {
	void accept(T t);
}
