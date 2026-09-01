# PoeScreen compile fix

Fixed `PoeScreen.java` `CleanerItemPickerScreen` from a `static` nested class to a non-static inner class.

Reason: the picker calls the parent `PoeScreen` instance methods `drawText(...)` and `fitText(...)`. The previous static nested class caused Java compile errors such as `non-static method ... cannot be referenced from a static context`.

No module logic was changed by this fix.
