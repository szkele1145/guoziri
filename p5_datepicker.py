import io
import os

BASE = r"C:\Users\一只屑\Desktop\HousewifeAppProto\android-native\app\src\main\java\com\guoziri\app"


def read(name):
    with io.open(os.path.join(BASE, name), encoding="utf-8") as f:
        return f.read()


def write(name, content):
    with io.open(os.path.join(BASE, name), "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    print("written:", name)


def sub(src, old, new, label):
    assert old in src, "NOT FOUND: " + label
    return src.replace(old, new, 1)


src = read("NutritionPage.kt")

# DatePickerSheet 需要传入有记录的日期
old = """        DatePickerSheet(
            initial = pickedDate,
            onPick = { pickedDate = it; showDatePicker = false },
            onDismiss = { showDatePicker = false },
        )"""
new = """        DatePickerSheet(
            initial = pickedDate,
            recordedDates = repo.purchases.map { it.date }.distinct(),
            onPick = { pickedDate = it; showDatePicker = false },
            onDismiss = { showDatePicker = false },
        )"""
src = sub(src, old, new, "NutritionPage datepicker recordedDates")

write("NutritionPage.kt", src)
print("OK")
