#!/usr/bin/perl
use strict;
use warnings;
use Getopt::Long;
use Cwd qw(getcwd);
use File::Find;
my $root_dir = getcwd;
$| = 1;
my ($regression, $riscv_test, $cpu_test, $am_test);
my $AM_dir = $root_dir."/AM";
my $riscv_test_dir = $AM_dir."/riscv-tests/build";
my $cpu_test_dir = $AM_dir."/am-kernels/tests/cpu-tests/build";
my $am_test_dir = $AM_dir."/am-kernels/tests/am-tests/build";
my $waveform_filename = $root_dir."/build/NUCPU.vcd";
my $regression_log_dir = $root_dir."/regression";
my $emu_file = $root_dir."/build/emu";

if (-e $regression_log_dir) {
    system("cd $regression_log_dir; rm -rf *");
} else {
    system("mkdir -p $regression_log_dir");
}

GetOptions (
    'regression|r' => \$regression,
    'rsicvtest|v'  => \$riscv_test,
    'cputest|c'    => \$cpu_test,
    'amtest|a'     => \$am_test
);

if (defined $regression) {
    $riscv_test = 1;
    $cpu_test = 1;
    $am_test = 1;
}
# update RTL
my $update_RTL = system("cd $root_dir; mill -i __.nucpu.tests.runMain nucpu.testers.Generator");
# update EMU
my $update_EMU = system("cd $root_dir; make -C dependencies/difftest clean; make -C dependencies/difftest emu EMU_TRACE=1");
# Regression
my @failed_tests;
if ($update_EMU == 0 && $update_RTL == 0) {
    printf("[INFO] Start Regression Tests.\n");
    if (defined $riscv_test) {
        do_regression_tests($riscv_test_dir);
    }
    if (defined($cpu_test)) {
        do_regression_tests($cpu_test_dir);
    }
    if (defined($am_test)) {
        do_regression_tests($am_test_dir);
    }
    printf("[INFO] Regression Test Finished. The failed cases are:\n");
    open(FAILEDFILES, ">", $regression_log_dir."/failed.txt") || die "Can't write failed filenames $!\n";
    foreach my $failed_file (@failed_tests) {
        printf($failed_file."\n");
        print FAILEDFILES $failed_file;
        print FAILEDFILES "\n";
    }
    close(FAILEDFILES);
}
else {
    printf("[ERROR] Rebuild RTL or EMU error!\n");
}

sub get_bin_files {
    my ($dir) = @_;
    my @file_list;
    find ( sub {
        return unless -f;       #Must be a file
        return unless /\.bin$/;
        push @file_list, $File::Find::name;
    }, $dir );
    return(@file_list);
}

sub do_regression_tests {
    my ($test_dir) = @_;
    chdir($test_dir);
    my @test_files = get_bin_files($test_dir);
    foreach my $test_file (@test_files) {
        my $test_result = run_test_case($test_file);
        if ($test_result != 0) {
            push(@failed_tests, $test_file);
        }
    }
}

sub run_test_case {
    my ($test_file) = @_;
    my @spl = split(/\//, $test_file);
    my @spl2 = split(/-riscv64-mycpu.bin/, $spl[-1]);
    my $test_name = $spl2[-1];
    printf("[INFO] Current Test is $test_name\n");
    my $test_result = system("$emu_file -i $test_file  --dump-wave -b 0 -e 2000000 > $regression_log_dir/$test_name.log");
    if ($test_result != 0) {
        printf("[ERROR] Test $test_file failed!\n");
    }
    system("mv $waveform_filename $regression_log_dir/$test_name.vcd");
    return($test_result);
}
