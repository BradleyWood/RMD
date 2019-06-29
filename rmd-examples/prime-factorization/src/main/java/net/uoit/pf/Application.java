package net.uoit.pf;

import java.math.BigInteger;
import java.util.HashSet;

public class Application {

    private static final BigInteger TWO = BigInteger.valueOf(2);

    public static void main(String[] args) {
//        if (args.length < 2) {
//            System.out.println("Usage: <num jobs> <prime number>");
//            return;
//        }

//        int numJobs = Integer.parseInt(args[0]);

        int numJobs = 4;



        long start = System.currentTimeMillis();

        for (BigInteger i : primeFactors(new BigInteger("2251797028667587"))) {
            System.out.println(i);
        }

        System.out.println("Elapsed: " + (System.currentTimeMillis() - start));
    }

    private static HashSet<BigInteger> distributedPrimeFactors(BigInteger n, int numJobs) {
        final HashSet<BigInteger> list = new HashSet<>();

        BigInteger from = BigInteger.valueOf(3);

        for (int i = 0; i < numJobs; i++) {
            // not distributed first...
            BigInteger space = n.divide(BigInteger.valueOf(numJobs));


        }

        return list;
    }

    private static HashSet<BigInteger> primeFactors(BigInteger n) {
        return primeFactors(BigInteger.valueOf(3), n);
    }

    private static HashSet<BigInteger> primeFactors(BigInteger from, BigInteger n) {
        final HashSet<BigInteger> list = new HashSet<>();

        while (n.mod(TWO).compareTo(BigInteger.ZERO) == 0) {
            list.add(TWO);
            n = n.divide(TWO);
        }

        if (from.mod(TWO).compareTo(BigInteger.ZERO) == 0) {
            from = from.subtract(BigInteger.ONE);
        }

        for (BigInteger i = from; i.compareTo(sqrt(n)) <= 0; i = i.add(TWO)) {
            while (n.mod(i).compareTo(BigInteger.ZERO) == 0) {
                list.add(i);
                n = n.divide(i);
            }
        }

        if (n.compareTo(TWO) > 0) {
            list.add(n);
        }

        return list;
    }

    private static BigInteger sqrt(BigInteger x) {
        BigInteger div = BigInteger.ZERO.setBit(x.bitLength() / 2);
        BigInteger div2 = div;

        for (; ; ) {
            BigInteger y = div.add(x.divide(div)).shiftRight(1);
            if (y.equals(div) || y.equals(div2))
                return y;
            div2 = div;
            div = y;
        }
    }
}
