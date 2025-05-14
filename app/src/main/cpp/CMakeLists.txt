cmake_minimum_required(VERSION 3.4.1)

add_library(
        usb_auth_native
        SHARED
        usb_auth.c
)

find_library(
        log-lib
        log
)

target_link_libraries(
        usb_auth_native
        ${log-lib}
)