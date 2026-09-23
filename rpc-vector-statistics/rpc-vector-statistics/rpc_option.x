struct data {
    int n;
    int Y<>;
};

struct product_data{
    int n;
    int Y<>;
    float a;
};

struct int_array{
    int array<>;
};

struct float_array{
    float array<>;
};

program OPTION {
    version OPTION_VERSION {
        float OPTION_MIDDLE(data) = 1;
        int_array OPTION_MIN_MAX(data) = 2;
        float_array OPTION_PRODUCT(product_data) = 3;
    } = 1;
} = 0x31230000;
