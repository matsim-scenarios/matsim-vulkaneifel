#!/bin/bash --login
#$ -l h_rt=790000
#$ -j y
#$ -m a
#$ -cwd
#$ -pe mp 12
#$ -l mem_free=5G
#$ -l ../logfile/logfile_calib.log
#$ -N calib-scenario

date
hostname

command="python -u calibrate.py"

echo ""
echo "command is $command"
echo ""

source env/bin/activate

module add java/17
java -version

$command
