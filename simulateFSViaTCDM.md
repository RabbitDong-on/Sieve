# Simulate Fail-slow hardwares
## Simulate Fail-slow NIC
Linux provides traffic control (TC) to control network
```
# install tc
apt-get install iproute2
apt-get install python3.7
rm /usr/bin/python
ln -s /usr/bin/python3.7 /usr/bin/python
apt-get install python3-pip
# install tcconfig
pip install tcconfig

# simulate network delay
tcset eth0 --dst-network/src-network 172.30.0.2 --delay 100 <ms>
tcdel eth0 --all
```
## Simulate Fail-slow disk
Linux provides Device Mapper to simulate disk fault in device layer.
```
# construct map
# allocate 100M file
dd if=/dev/zero of=/tmp/100M-of-zeroes bs=1024k count=100

# find available device
losetup -f
-> /dev/loop31
losetup /dev/loop31 /tmp/100M-of-zeros

# get offset
blockdev --getsize /dev/loop29

# create mapper
echo "0 204800 delay /dev/loop29 0 200" | dmsetup create dm-slow

# Test the simulated fault
dd if=/dev/mapper/dm-slow of=/dev/null count=25000
dd if=/dev/loop31 of=/dev/null count=25000

# mount fs
mkfs.ext4 /dev/mapper/dm-slow
mkdir -p /mnt/slow
mount -o sync /dev/mapper/dm-slow /mnt/slow
```