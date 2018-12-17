#!/bin/sh

nohup sh one.sh -b 1 default_settings_auction.txt > outAuction & 
# nohup sh one.sh -b 1 default_settings_greedy.txt > outGreedy &
nohup sh one.sh -b 1 default_settings_auction_NoMigration.txt > outNoMigration &
