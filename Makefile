build-image:
	chmod +x *.sh
	docker network create --driver bridge --subnet 172.30.0.0/16 --gateway 172.30.0.1 Sieve_net
	docker image build -t xx/failslow:0.1 .

run-container:
	docker container run -it --privileged --net=Sieve_net --name $(container_name) xx/failslow:0.1 ./addid.sh $(id)

run-hbasecontainer:
	docker container run -it --privileged --net=Sieve_net --name $(container_name) --hostname $(host_name) xx/failslow:0.1 ./addid.sh $(id)

clean: rm-container rm-image rm-network

rm-container:
	docker container rm --force failslow1
	docker container rm --force failslow2
	docker container rm --force failslow3
	docker container rm --force failslow4

rm-image:
	docker image rm xx/failslow:0.1

rm-network:
	docker network rm Sieve_net
