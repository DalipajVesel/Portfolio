struct vectors{
int n;
int x<>;
int y<>;
};

struct mean_result{
double mean_x;
double mean_y;
};

struct product_vector{
int n;
double r;
int x<>;
};

struct product_result{
int n;
double values<>;
};



program DS_PROG{
	version DS_VERS{
		int DOT_PRODUCT_CALCULATOR(vectors) = 1;
		mean_result MEAN_CALCULATOR(vectors) = 2;
		product_result PRODUCT_CALCULATOR(product_vector) = 3;
	} = 1;
} = 0x20000001;

