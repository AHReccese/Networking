#include "ns3/core-module.h"


using namespace ns3 ;
NS_LOG_COMPONENT_DEFINE("Lab#3");

int main (int argc , char * argv[]) {
int a ;
int b ;
CommandLine myCMD;
myCMD.AddValue("a", "This is a", a ) ;
myCMD.AddValue("b", "This is b", b ) ;
myCMD.Parse (argc , argv ) ;
std::cout << " a + b = " << a + b ;
return 0;
}
