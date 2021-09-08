#!/usr/bin/perl
use strict;
use warnings;
use Getopt::Long;
use Cwd qw(getcwd);
use File::Find;
use Term::ANSIColor qw(:constants);

my $root_dir = getcwd;
$| = 1;
my ($regression, $riscv_test, $cpu_test, $am_test, $mario_test, $interrupt_test);
# AM Tests
my ($time_test, $yield_test, $hello_test);
my $AM_dir = $root_dir."/AM";
my $oscpu_bin_dir = $AM_dir."/oscpu-framework/bin";
my $riscv_test_dir = $oscpu_bin_dir."/non-output/riscv-tests";
my $cpu_test_dir = $oscpu_bin_dir."/non-output/cpu-tests";
my $hello_test_dir = $oscpu_bin_dir."/custom-output/hello";
my $yield_test_dir = $oscpu_bin_dir."/custom-output/yield-test";
my $time_test_dir = $oscpu_bin_dir."/custom-output/time-test";
my $interrupt_test_dir = $oscpu_bin_dir."/custom-output/interrupt-test";
my $mario_test_dir = $oscpu_bin_dir."/custom-output/mario";
#
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
    'amtest|a'     => \$am_test,
    'mario|m'      => \$mario_test,
    'timetest|t'   => \$time_test,
    'yieldtest|y'  => \$yield_test,
    'interrupt|i'  => \$interrupt_test,
);

if (defined $regression) {
    $riscv_test = 1;
    $cpu_test = 1;
    $am_test = 1;
    $mario_test = 1;
}
if (defined($am_test)) {
    $time_test = 1;
    $yield_test = 1;
    $hello_test = 1;
    $interrupt_test = 1;
}

my @failed_tests;
if (defined($riscv_test) || defined($cpu_test) || defined($am_test) ||
    defined($mario_test) || defined($time_test) || defined($yield_test) || defined($hello_test) || defined($interrupt_test)) {
    # update RTL
    my $update_RTL = system("cd $root_dir; mill -i __.nucpu.tests.runMain nucpu.testers.Generator");
    # update EMU
    my $update_EMU = system("cd $root_dir; make -C dependencies/difftest clean; make -C dependencies/difftest emu EMU_TRACE=1");
    # Regression
    if ($update_EMU == 0 && $update_RTL == 0) {
        printf("[INFO] Start Regression Tests.\n");
        if (defined $riscv_test) {
            do_regression_tests($riscv_test_dir);
        }
        if (defined($cpu_test)) {
            do_regression_tests($cpu_test_dir);
        }
        if (defined($hello_test)) {
            do_regression_tests($hello_test_dir);
        }
        if (defined($time_test)) {
            do_regression_tests($time_test_dir);
        }
        if (defined($yield_test)) {
            do_regression_tests($yield_test_dir);
        }
        if (defined($interrupt_test)) {
            do_regression_tests($interrupt_test_dir);
        }
        if (defined($mario_test)) {
            do_regression_tests($mario_test_dir);
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
        print RED, "[ERROR] Rebuild RTL or EMU error!\n", RESET;
    }
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
    my ($test_dir, $postfix) = @_;
    chdir($test_dir);
    my @test_files = get_bin_files($test_dir);
    foreach my $test_file (@test_files) {
        my $test_result = run_test_case($test_file, $postfix);
        if ($test_result == 0) {
            printf("[INFO] Test $test_file Passed!\n");
        }
        else {
            if ($test_result == 256) {
                print RED, "[ERROR] Test $test_file failed!\n", RESET;
                if (defined($postfix)) {
                    $test_file = $test_file."_".$postfix;
                }
                push(@failed_tests, $test_file);
            }
            else {
                print BRIGHT_MAGENTA, "\n[WARNING] Test $test_file may fail $test_result!\n", RESET;
            }
        }
    }
}

sub run_test_case {
    my ($test_file, $postfix) = @_;
    my @spl = split(/\//, $test_file);
    my @spl2 = split(/.bin/, $spl[-1]);
    my $test_name = $spl2[-1];
    if (defined($postfix)) {
        $test_name = $test_name."_".$postfix;
    }
    printf("[INFO] Current Test is $test_name\n");
    my $test_result = system("$emu_file -i $test_file --dump-wave -b 0 -e 2000000 > $regression_log_dir/$test_name.log");
    system("mv $waveform_filename $regression_log_dir/$test_name.vcd");
    return($test_result);
}
