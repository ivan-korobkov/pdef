using System;
using System.Collections.Generic;

namespace Pdef.test
{
    public enum Enum
    {
        FIRST,
        SECOND,
        THIRD
    }


    public partial class Exc0 : Exception
    {
    }


    public partial class Exc1 : Exception
    {
    }


    public interface IInterface0 {
    }


    public partial class Message0
    {
    }


    [SubType("first", typeof(SubMessage1))]
    [SubType("second", typeof(SubMessage2))]
    [SubType("third", typeof(SubMessage3))]
    public partial class Message
    {
        public Enum Type_field { get; set; }
        public bool Bool_field { get; set; }
        public short Int16_field { get; set; }
        public int Int32_field { get; set; }
        public long Int64_field { get; set; }
        public float Float_field { get; set; }
        public double Double_field { get; set; }
        public decimal Decimal_field { get; set; }
        public date Date_field { get; set; }
        public datetime Datetime_field { get; set; }
        public string String_field { get; set; }
        public uuid Uuid_field { get; set; }
        public object Object_field { get; set; }
        public IList<SubMessage1> List_field { get; set; }
        public ISet<SubMessage2> Set_field { get; set; }
        public IDictionary<string, SubMessage3> Map_field { get; set; }
    }


    public partial class SubMessage1 : Message
    {
        public Message Sub_field1 { get; set; }
        public string Sub_field2 { get; set; }
    }


    public partial class SubMessage2 : Message
    {
        public int Sub_field1 { get; set; }
    }


    public partial class SubMessage3 : SubMessage2
    {
        public int Sub_field2 { get; set; }
    }


    public partial class Exception : Exception
    {
        public string Code { get; set; }
    }


    public interface IInterface {
        void Method();
    
        IObservable<int> Sum(int i0, int i1);
    
        IObservable<string> Echo(string text);
    
    }


    public interface ISupport {
        void CallSupport();
    
    }


    public interface ISubInterface : IInterface, ISupport {
        IObservable<SubMessage3> Submethod(SubMessage1 msg1, SubMessage2 msg2);
    
    }


}
