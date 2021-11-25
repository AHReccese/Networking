 #include "ns3/core-module.h"
 #include "ns3/network-module.h"
 #include "ns3/csma-module.h"
 #include "ns3/applications-module.h"
 #include "ns3/internet-module.h"
 #include "ns3/flow-monitor-module.h"
 #include "ns3/netanim-module.h"
 #include "ns3/mobility-module.h"
 

 using namespace ns3;

NS_LOG_COMPONENT_DEFINE ("CsmaExample");

int
main (int argc, char *argv[])
{
  float Interval,PacketSize;
  float Thput1,Thput2,Thput3,Thput4;
  float Delay1,Delay2,Delay3,Delay4,Delay5,Delay6,Delay7,Delay8;

  CommandLine cmd;
  cmd.AddValue("Interval", "This is Interval", Interval ) ;
  cmd.AddValue("PacketSize", "This is PacketSize", PacketSize ) ;
  cmd.Parse (argc, argv);
 
  Time::SetResolution (Time::NS);
  LogComponentEnable ("UdpEchoClientApplication", LOG_LEVEL_INFO);
  LogComponentEnable ("UdpEchoServerApplication", LOG_LEVEL_INFO);


  NodeContainer node;
  node.Create (8);

  CsmaHelper csma;
  csma.SetChannelAttribute ("DataRate", StringValue ("1024Kbps"));
  csma.SetChannelAttribute ("Delay", StringValue ("2ms"));
  NetDeviceContainer devices = csma.Install (node);
  
  InternetStackHelper internet;
  internet.Install (node);
  
  Ipv4AddressHelper address;
  address.SetBase ("11.63.5.0", "255.255.255.0");
  Ipv4InterfaceContainer interfaces = address.Assign (devices);

  // 1
  
  UdpEchoServerHelper echoServer (9);

  ApplicationContainer apps = echoServer.Install (node.Get (4));
  apps.Start (Seconds (1.0));
  apps.Stop (Seconds (10.0));

  UdpEchoClientHelper echoClient (interfaces.GetAddress (4), 9);
  echoClient.SetAttribute ("MaxPackets", UintegerValue (1));
  echoClient.SetAttribute ("Interval", TimeValue (Seconds (Interval)));
  echoClient.SetAttribute ("PacketSize", UintegerValue (PacketSize));

  ApplicationContainer clientApps = echoClient.Install (node.Get (0));
  clientApps.Start (Seconds (2.0));
  clientApps.Stop (Seconds (10.0));

// 2
  UdpEchoServerHelper echoServer2 (9);

  ApplicationContainer apps2 = echoServer2.Install (node.Get (5));
  apps2.Start (Seconds (1.0));
  apps2.Stop (Seconds (10.0));
  
  UdpEchoClientHelper echoClient2 (interfaces.GetAddress (5), 9);
  echoClient2.SetAttribute ("MaxPackets", UintegerValue (1));
  echoClient2.SetAttribute ("Interval", TimeValue (Seconds (Interval)));
  echoClient2.SetAttribute ("PacketSize", UintegerValue (PacketSize));

  ApplicationContainer clientApps2 = echoClient2.Install (node.Get (1));
  clientApps2.Start (Seconds (2.0));
  clientApps2.Stop (Seconds (10.0));

// 3
  UdpEchoServerHelper echoServer3 (9);

  ApplicationContainer apps3 = echoServer3.Install (node.Get (2));
  apps3.Start (Seconds (1.0));
  apps3.Stop (Seconds (10.0));

  UdpEchoClientHelper echoClient3 (interfaces.GetAddress (2), 9);
  echoClient3.SetAttribute ("MaxPackets", UintegerValue (1));
  echoClient3.SetAttribute ("Interval", TimeValue (Seconds (Interval)));
  echoClient3.SetAttribute ("PacketSize", UintegerValue (PacketSize));

  ApplicationContainer clientApps3 = echoClient3.Install (node.Get (6));
  clientApps3.Start (Seconds (2.0));
  clientApps3.Stop (Seconds (10.0));




// 4
  UdpEchoServerHelper echoServer4 (9);

  ApplicationContainer apps4 = echoServer4.Install (node.Get (3));
  apps4.Start (Seconds (1.0));
  apps4.Stop (Seconds (10.0));

  UdpEchoClientHelper echoClient4 (interfaces.GetAddress (3), 9);
  echoClient4.SetAttribute ("MaxPackets", UintegerValue (1));
  echoClient4.SetAttribute ("Interval", TimeValue (Seconds (Interval)));
  echoClient4.SetAttribute ("PacketSize", UintegerValue (PacketSize));

  ApplicationContainer clientApps4 = echoClient4.Install (node.Get (7));
  clientApps4.Start (Seconds (2.0));
  clientApps4.Stop (Seconds (10.0));



// Tracing
  AsciiTraceHelper ascii;
  csma.EnableAscii(ascii.CreateFileStream ("HW3.tr"), devices);
  csma.EnablePcap("HW3", devices, false);

// Calculate Throughput using Flowmonitor
  FlowMonitorHelper myflow;
  Ptr<FlowMonitor> monitor = myflow.InstallAll();
  
// Animation

  MobilityHelper mobility;
  mobility.SetPositionAllocator ("ns3::GridPositionAllocator",
            "MinX", DoubleValue (0.0),
			"MinY", DoubleValue (0.0),
			"DeltaX", DoubleValue (10.0),
			"DeltaY", DoubleValue (10.0),
            "GridWidth", UintegerValue (5),
			"LayoutType", StringValue ("RowFirst"));

  mobility.SetMobilityModel ("ns3::ConstantPositionMobilityModel");
  mobility.Install (node);

  AnimationInterface anim ("animation.xml");
  anim.SetConstantPosition (node.Get (0), 0.0, 20.0);
  anim.SetConstantPosition (node.Get (1), 5.0, 20.0);
  anim.SetConstantPosition (node.Get (2), 10.0, 20.0);
  anim.SetConstantPosition (node.Get (3), 15.0, 20.0);
  anim.SetConstantPosition (node.Get (4), 20.0, 20.0);
  anim.SetConstantPosition (node.Get (5), 25.0, 20.0);
  anim.SetConstantPosition (node.Get (6), 30.0, 20.0);
  anim.SetConstantPosition (node.Get (7), 35.0, 20.0);
  
  Simulator::Stop (Seconds(10.0));
  Simulator::Run ();

  Ptr<Ipv4FlowClassifier> classifier = DynamicCast<Ipv4FlowClassifier> (myflow.GetClassifier ());
  std::map<FlowId, FlowMonitor::FlowStats> stats = monitor->GetFlowStats ();
  

  for (std::map<FlowId, FlowMonitor::FlowStats>::const_iterator i = stats.begin (); i != stats.end (); ++i)
    {
	  Ipv4FlowClassifier::FiveTuple t = classifier->FindFlow (i->first);
      if ((t.sourceAddress=="11.63.5.5" && t.destinationAddress == "11.63.5.1"))
      {
          std::cout << "Flow " << i->first  << " (" << t.sourceAddress << " -> " << t.destinationAddress << ")\n";
          std::cout << "  Tx Bytes:   " << i->second.txBytes << "\n";
          std::cout << "  Rx Bytes:   " << i->second.rxBytes << "\n";
      	  std::cout << "  FlowThroughput: " << i->second.rxBytes * 8.0 / (i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds())/1e6  << " Mbps\n";
          std::cout << "  FlowDelay: " << (i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds())  << " Second\n";
          Delay5= i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds();
      }

      if ((t.sourceAddress=="11.63.5.1" && t.destinationAddress == "11.63.5.5"))
      {
          std::cout << "Flow " << i->first  << " (" << t.sourceAddress << " -> " << t.destinationAddress << ")\n";
          std::cout << "  Tx Bytes:   " << i->second.txBytes << "\n";
          std::cout << "  Rx Bytes:   " << i->second.rxBytes << "\n";
      	  std::cout << "  FlowThroughput: " << i->second.rxBytes * 8.0 / (i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds())/1e6  << " Mbps\n";
          std::cout << "  FlowDelay: " << (i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds())  << " Second\n";
          Delay1= i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds();
          Thput1= i->second.rxBytes * 8.0 / (i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds())/1e6  ;
      } 

      if ((t.sourceAddress=="11.63.5.6" && t.destinationAddress == "11.63.5.2"))
      {
          std::cout << "Flow " << i->first  << " (" << t.sourceAddress << " -> " << t.destinationAddress << ")\n";
          std::cout << "  Tx Bytes:   " << i->second.txBytes << "\n";
          std::cout << "  Rx Bytes:   " << i->second.rxBytes << "\n";
      	  std::cout << "  FlowThroughput: " << i->second.rxBytes * 8.0 / (i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds())/1e6  << " Mbps\n";
          std::cout << "  FlowDelay: " << (i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds())  << " Second\n";
          Delay6= i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds();
      }

      if ((t.sourceAddress=="11.63.5.2" && t.destinationAddress == "11.63.5.6"))
      {
          std::cout << "Flow " << i->first  << " (" << t.sourceAddress << " -> " << t.destinationAddress << ")\n";
          std::cout << "  Tx Bytes:   " << i->second.txBytes << "\n";
          std::cout << "  Rx Bytes:   " << i->second.rxBytes << "\n";
      	  std::cout << "  FlowThroughput: " << i->second.rxBytes * 8.0 / (i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds())/1e6  << " Mbps\n";
          std::cout << "  FlowDelay: " << (i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds())  << " Second\n";
          Delay2= i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds();
          Thput2= i->second.rxBytes * 8.0 / (i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds())/1e6  ;
      }

      if ((t.sourceAddress=="11.63.5.3" && t.destinationAddress == "11.63.5.7"))
      {
          std::cout << "Flow " << i->first  << " (" << t.sourceAddress << " -> " << t.destinationAddress << ")\n";
          std::cout << "  Tx Bytes:   " << i->second.txBytes << "\n";
          std::cout << "  Rx Bytes:   " << i->second.rxBytes << "\n";
      	  std::cout << "  FlowThroughput: " << i->second.rxBytes * 8.0 / (i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds())/1e6  << " Mbps\n";
          std::cout << "  FlowDelay: " << (i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds())  << " Second\n";
          Delay7= i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds();
      }

      if ((t.sourceAddress=="11.63.5.7" && t.destinationAddress == "11.63.5.3"))
      {
          std::cout << "Flow " << i->first  << " (" << t.sourceAddress << " -> " << t.destinationAddress << ")\n";
          std::cout << "  Tx Bytes:   " << i->second.txBytes << "\n";
          std::cout << "  Rx Bytes:   " << i->second.rxBytes << "\n";
      	  std::cout << "  FlowThroughput: " << i->second.rxBytes * 8.0 / (i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds())/1e6  << " Mbps\n";
          std::cout << "  FlowDelay: " << (i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds())  << " Second\n";
          Delay3= i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds();
          Thput3= i->second.rxBytes * 8.0 / (i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds())/1e6  ;
      }

      if ((t.sourceAddress=="11.63.5.4" && t.destinationAddress == "11.63.5.8"))
      {
          std::cout << "Flow " << i->first  << " (" << t.sourceAddress << " -> " << t.destinationAddress << ")\n";
          std::cout << "  Tx Bytes:   " << i->second.txBytes << "\n";
          std::cout << "  Rx Bytes:   " << i->second.rxBytes << "\n";
      	  std::cout << "  FlowThroughput: " << i->second.rxBytes * 8.0 / (i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds())/1e6  << " Mbps\n";
          std::cout << "  FlowDelay: " << (i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds())  << " Second\n";
          Delay8= i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds();
      }  

      if ((t.sourceAddress=="11.63.5.8" && t.destinationAddress == "11.63.5.4"))
      {
          std::cout << "Flow " << i->first  << " (" << t.sourceAddress << " -> " << t.destinationAddress << ")\n";
          std::cout << "  Tx Bytes:   " << i->second.txBytes << "\n";
          std::cout << "  Rx Bytes:   " << i->second.rxBytes << "\n";
      	  std::cout << "  FlowThroughput: " << i->second.rxBytes * 8.0 / (i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds())/1e6  << " Mbps\n";
          std::cout << "  FlowDelay: " << (i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds())  << " Second\n";
          Delay4= i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds();
          Thput4= i->second.rxBytes * 8.0 / (i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds())/1e6  ;
      }  


     }

// flowmon folder

  monitor->SerializeToXmlFile("HW3.flowmon", true, true); 


      std::cout << "  Total FlowThroughput: " << (Thput1+Thput2+Thput3+Thput4)/4  << " Mbps\n";
      std::cout << "  Total FlowDelay one direction: " << (Delay1+Delay2+Delay3+Delay4+Delay5+Delay6+Delay7+Delay8)/8  << " Second\n";
      std::cout << "  Total FlowDelay end to end: " << (Delay1+Delay2+Delay3+Delay4+Delay5+Delay6+Delay7+Delay8)/4  << " Second\n";
     
       
   Simulator::Destroy ();
  return 0;
}
